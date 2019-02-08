/*	gc_n64_usb : Gamecube or N64 controller to USB adapter firmware
	Copyright (C) 2007-2017  Raphael Assenat <raph@raphnet.net>

	This program is free software: you can redistribute it and/or modify
	it under the terms of the GNU General Public License as published by
	the Free Software Foundation, either version 3 of the License, or
	(at your option) any later version.

	This program is distributed in the hope that it will be useful,
	but WITHOUT ANY WARRANTY; without even the implied warranty of
	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
	GNU General Public License for more details.

	You should have received a copy of the GNU General Public License
	along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <stdint.h>
#include "gcn64.h"
#include "gcn64_priv.h"
#include "gcn64lib.h"
#include "requests.h"

#include "libusb.h"

#include <android/log.h>
#define ANDROID_LOG_TAG "raphnet"
#define printf(...) __android_log_print(ANDROID_LOG_DEBUG, ANDROID_LOG_TAG, __VA_ARGS__)
#define fprintf(stream, ...) __android_log_print(ANDROID_LOG_ERROR, ANDROID_LOG_TAG, __VA_ARGS__)

static int dusbr_verbose = 1;

#define MAX_GCN64_DEVICES	4

struct usbdevice {
	int vid, pid, interface;
	int fd;
	libusb_device *dev;
};
static struct usbdevice s_devices[MAX_GCN64_DEVICES];
static int s_n_devices = 0;

static libusb_context *s_libusb_context;

#define IS_VERBOSE()	(dusbr_verbose)

struct supported_adapter {
	uint16_t vid, pid;
	int if_number;
	struct gcn64_adapter_caps caps;
};

static struct supported_adapter supported_adapters[] = {
	/* vid, pid, if_no, { n_raw, bio_support } */

	{ OUR_VENDOR_ID, 0x0017, 1, { 1, 0 } },	// GC/N64 USB v3.0, 3.1.0, 3.1.1
	{ OUR_VENDOR_ID, 0x001D, 1, { 1, 0 } }, // GC/N64 USB v3.2.0 ... v3.3.x
	{ OUR_VENDOR_ID, 0x0020, 1, { 1, 0 } }, // GCN64->USB v3.2.1 (N64 mode)
	{ OUR_VENDOR_ID, 0x0021, 1, { 1, 0 } }, // GCN64->USB v3.2.1 (GC mode)
	{ OUR_VENDOR_ID, 0x0022, 1, { 2, 0 } }, // GCN64->USB v3.3.x (2x GC/N64 mode)
	{ OUR_VENDOR_ID, 0x0030, 1, { 2, 0 } }, // GCN64->USB v3.3.0 (2x N64-only mode)
	{ OUR_VENDOR_ID, 0x0031, 1, { 2, 0 } }, // GCN64->USB v3.3.0 (2x GC-only mode)

	{ OUR_VENDOR_ID, 0x0032, 1, { 1, 1 } }, // GC/N64 USB v3.4.x (GC/N64 mode)
	{ OUR_VENDOR_ID, 0x0033, 1, { 1, 1 } }, // GC/N64 USB v3.4.x (N64 mode)
	{ OUR_VENDOR_ID, 0x0034, 1, { 1, 1 } }, // GC/N64 USB v3.4.x (GC mode)
	{ OUR_VENDOR_ID, 0x0035, 1, { 2, 1 } }, // GC/N64 USB v3.4.x (2x GC/N64 mode)
	{ OUR_VENDOR_ID, 0x0036, 1, { 2, 1 } }, // GC/N64 USB v3.4.x (2x N64-only mode)
	{ OUR_VENDOR_ID, 0x0037, 1, { 2, 1 } }, // GC/N64 USB v3.4.x (2x GC-only mode)

	// GC/N64 USB v3.5.x flavours
	{ OUR_VENDOR_ID, 0x0038, 1, { 1, 1 } }, // (GC/N64 mode)
	{ OUR_VENDOR_ID, 0x0039, 1, { 1, 1 } }, // (N64 mode)
	{ OUR_VENDOR_ID, 0x003A, 1, { 1, 1 } }, // (GC mode)
	{ OUR_VENDOR_ID, 0x003B, 2, { 2, 1 } }, // (2x GC/N64 mode)
	{ OUR_VENDOR_ID, 0x003C, 2, { 2, 1 } }, // (2x N64-only mode)
	{ OUR_VENDOR_ID, 0x003D, 2, { 2, 1 } }, // (2x GC-only mode)

	// GC/N64 USB v3.6.x flavours
	{ OUR_VENDOR_ID, 0x0060, 1, { 1, 1 } }, // (GC/N64 mode)
	{ OUR_VENDOR_ID, 0x0061, 1, { 1, 1 } }, // (N64 mode)
	{ OUR_VENDOR_ID, 0x0063, 2, { 2, 1 } }, // (2x GC/N64 mode)
	{ OUR_VENDOR_ID, 0x0064, 2, { 2, 1 } }, // (2x N64-only mode)
	{ OUR_VENDOR_ID, 0x0067, 1, { 1, 1 } }, // (GC/N64 in GC keyboard mode)

	{ }, // terminator
};

static int getInterfaceToUseForVidPid(int vid, int pid)
{
	struct supported_adapter *adap = supported_adapters;

	for (adap = supported_adapters; adap->vid; adap++) {
		if ((adap->vid == vid) && ((adap->pid == pid))) {
			return adap->if_number;
		}
	}

	return -1;
}

static char isProductIdHandled(unsigned short pid, int interface_number, struct gcn64_adapter_caps *caps)
{
	int i;

	for (i=0; supported_adapters[i].vid; i++) {
		if (pid == supported_adapters[i].pid) {
			if (interface_number == supported_adapters[i].if_number) {
				if (caps) {
					memcpy(caps, &supported_adapters[i].caps, sizeof (struct gcn64_adapter_caps));
				}
				return 1;
			}
		}
	}

	return 0;
}


// This is called before gcn64_init
int gcn64_android_addDevice(int vid, int pid, int fd)
{
	int interface;

	if (s_n_devices >= MAX_GCN64_DEVICES) {
		fprintf(stderr, "Cannot add device (no space left)\n");
		return -1;
	}

	interface = getInterfaceToUseForVidPid(vid, pid);
	if (interface < 0) {
		printf("Rejecting device %04x:%04x (not supported)\n", vid, pid);
		return -1;
	}

	s_devices[s_n_devices].dev = NULL;
	s_devices[s_n_devices].vid = vid;
	s_devices[s_n_devices].pid = pid;
	s_devices[s_n_devices].fd = fd;
	s_devices[s_n_devices].interface = interface;
	printf("Added device %04x:%04x(%d) with fd %d at index %d\n", vid, pid, interface, fd, s_n_devices);
	s_n_devices++;

	return 0;
}

int gcn64_init(int verbose)
{
	int i, ret;
	libusb_device *dev;

	ret = libusb_init(&s_libusb_context);
	if (ret) {
		fprintf(stderr ,"libusb init failed: %s\n", libusb_strerror(ret));
		return -1;
	}

	// Now that libusb is ready, obtain the libusb_device for each vid/pid/fd we were
	// told about in gcn64_android_addDevice()

	for (i=0; i<s_n_devices; i++) {
		struct usbdevice *d = &s_devices[i];

		dev = libusb_get_device_with_fd(s_libusb_context, d->vid, d->pid, "SERIAL", d->fd, 0, 0);
		if (dev) {
			printf("Got libusb_device for index %d\n", i);
			s_devices[i].dev = dev;
		}
	}

	printf("gcn64_init done\n");

	dusbr_verbose = verbose;

	return 0;
}

void gcn64_shutdown(void)
{
	libusb_exit(s_libusb_context);
}

struct gcn64_list_ctx *gcn64_allocListCtx(void)
{
	struct gcn64_list_ctx *ctx;
	ctx = calloc(1, sizeof(struct gcn64_list_ctx));
	return ctx;
}

void gcn64_freeListCtx(struct gcn64_list_ctx *ctx)
{
	if (ctx) {
		free(ctx);
	}
}

int gcn64_countDevices(void)
{
	struct gcn64_list_ctx *ctx;
	struct gcn64_info inf;
	int count = 0;

	ctx = gcn64_allocListCtx();
	while (gcn64_listDevices(&inf, ctx)) {
		count++;
	}
	gcn64_freeListCtx(ctx);

	return count;
}

/**
 * \brief List instances of our rgbleds device on the USB busses.
 * \param info Pointer to gcn64_info structure to store data
 * \param dst Destination buffer for device serial number/id.
 * \param dstbuf_size Destination buffer size.
 */
struct gcn64_info *gcn64_listDevices(struct gcn64_info *info, struct gcn64_list_ctx *ctx)
{
	struct gcn64_adapter_caps caps;
	struct usbdevice *d;

	memset(info, 0, sizeof(struct gcn64_info));

	if (!ctx) {
		fprintf(stderr, "gcn64_listDevices: Passed null context\n");
		return NULL;
	}

	// End of enumeration
	if (ctx->index >= s_n_devices) {
		return NULL;
	}

	d = &s_devices[ctx->index];

	if (isProductIdHandled(d->pid, d->interface, &caps))
	{
		memset(info, 0, sizeof(struct gcn64_info)); // Clear all fields

		info->usb_vid = d->vid;
		info->usb_pid = d->pid;
		info->access = 1;
		info->index = ctx->index;
		memcpy(&info->caps, &caps, sizeof(info->caps));

		ctx->index++;

		return info;
	}

	ctx->index++;

	return NULL;
}

extern int g_vid, g_pid;

gcn64_hdl_t gcn64_openDevice(struct gcn64_info *dev)
{
	gcn64_hdl_t hdl;
	int res;

	if (!dev)
		return NULL;

	if (IS_VERBOSE()) {
		printf("'Opening' device index %d'\n", dev->index);
	}

	hdl = malloc(sizeof(struct _gcn64_hdl_t));
	if (!hdl) {
		perror("malloc");
		return NULL;
	}

	hdl->report_size = 63;
	hdl->interface = s_devices[dev->index].interface;
	memcpy(&hdl->caps, &dev->caps, sizeof(hdl->caps));

	res = libusb_open(s_devices[dev->index].dev, &hdl->device_handle);
	if (res) {
		fprintf(stderr, "libusb_open failed : %s\n", libusb_strerror(res));
		free(hdl);
		return NULL;
	}

	libusb_set_auto_detach_kernel_driver(hdl->device_handle, 1);

	res = libusb_claim_interface(hdl->device_handle, hdl->interface);
	if (res) {
		fprintf(stderr, "failed to claim interface : %s\n", libusb_strerror(res));
		libusb_close(hdl->device_handle);
		free(hdl);
		return NULL;
	}

	if (!dev->caps.bio_support) {
		printf("Pre-3.4 version detected. Setting report size to 40 bytes\n");
		hdl->report_size = 40;
	}

	return hdl;
}

gcn64_hdl_t gcn64_openBy(struct gcn64_info *dev, unsigned char flags)
{
	struct gcn64_list_ctx *ctx;
	struct gcn64_info inf;
	gcn64_hdl_t h;

	if (IS_VERBOSE())
		printf("gcn64_openBy, flags=0x%02x\n", flags);

	ctx = gcn64_allocListCtx();
	if (!ctx)
		return NULL;

	while (gcn64_listDevices(&inf, ctx)) {
		if (IS_VERBOSE())
			printf("Considering '%s'\n", inf.str_path);

		if (flags & GCN64_FLG_OPEN_BY_SERIAL) {
			if (wcscmp(inf.str_serial, dev->str_serial))
				continue;
		}

		if (flags & GCN64_FLG_OPEN_BY_PATH) {
			if (strcmp(inf.str_path, dev->str_path))
				continue;
		}

		if (flags & GCN64_FLG_OPEN_BY_VID) {
			if (inf.usb_vid != dev->usb_vid)
				continue;
		}

		if (flags & GCN64_FLG_OPEN_BY_PID) {
			if (inf.usb_pid != dev->usb_pid)
				continue;
		}

		if (IS_VERBOSE())
			printf("Found device. opening...\n");

		h = gcn64_openDevice(&inf);
		gcn64_freeListCtx(ctx);
		return h;
	}

	gcn64_freeListCtx(ctx);
	return NULL;
}

void gcn64_closeDevice(gcn64_hdl_t hdl)
{
	if (hdl) {
		if (hdl->device_handle) {
			libusb_release_interface(hdl->device_handle, hdl->interface);
			libusb_close(hdl->device_handle);
		}
		free(hdl);
	}
}

// Copied from hidapi
static int gcn64_hid_send_feature_report(gcn64_hdl_t hdl, const unsigned char *data, size_t length)
{
	int res = -1;
	int skipped_report_id = 0;
	int report_number = data[0];

	if (report_number == 0x0) {
		data++;
		length--;
		skipped_report_id = 1;
	}

	res = libusb_control_transfer(hdl->device_handle,
			LIBUSB_REQUEST_TYPE_CLASS|LIBUSB_RECIPIENT_INTERFACE|LIBUSB_ENDPOINT_OUT,
			0x09/*HID set_report*/,
			(3/*HID feature*/ << 8) | report_number,
			hdl->interface,
			(unsigned char *)data, length,
			1000/*timeout millis*/);

	if (res < 0) {
		fprintf(stderr, "libusb_control_transfer: %s\n", libusb_strerror(res));
		return -1;
	}

	/* Account for the report ID */
	if (skipped_report_id)
		length++;

	return length;
}

int gcn64_send_cmd(gcn64_hdl_t hdl, const unsigned char *cmd, int cmdlen)
{
	unsigned char buffer[hdl->report_size+1];
	int n;

	if (cmdlen > (sizeof(buffer)-1)) {
		fprintf(stderr, "Error: Command too long\n");
		return -1;
	}

	memset(buffer, 0, sizeof(buffer));

	buffer[0] = 0x00; // report ID set to 0 (device has only one)
	memcpy(buffer + 1, cmd, cmdlen);

	n = gcn64_hid_send_feature_report(hdl, buffer, sizeof(buffer));
	if (n < 0) {
		fprintf(stderr, "Could not send feature report)\n");
		return -1;
	}
	return 0;
}

// Copied from hidapi
static int gcn64_hid_get_feature_report(gcn64_hdl_t hdl, unsigned char *data, size_t length)
{
	int res = -1;
	int skipped_report_id = 0;
	int report_number = data[0];

	if (report_number == 0x0) {
		/* Offset the return buffer by 1, so that the report ID
		 * will remain in byte 0. */
		data++;
		length--;
		skipped_report_id = 1;
	}

	res = libusb_control_transfer(hdl->device_handle,
		LIBUSB_REQUEST_TYPE_CLASS|LIBUSB_RECIPIENT_INTERFACE|LIBUSB_ENDPOINT_IN,
		0x01/*HID get_report*/,
		(3/*HID feature*/ << 8) | report_number,
		hdl->interface,
		(unsigned char *)data, length,
		1000/*timeout millis*/);

	if (res < 0)
		return -1;

	if (skipped_report_id)
		res++;

	return res;
}

int gcn64_poll_result(gcn64_hdl_t hdl, unsigned char *cmd, int cmd_maxlen)
{
	unsigned char buffer[hdl->report_size+1];
	int res_len;
	int n;

	memset(buffer, 0, sizeof(buffer));
	buffer[0] = 0x00; // report ID set to 0 (device has only one)

	n = gcn64_hid_get_feature_report(hdl, buffer, sizeof(buffer));
	if (n < 0) {
		fprintf(stderr, "Could not get feature report\n");
		return -1;
	}
	if (n==0) {
		return 0;
	}
	res_len = n-1;

	if (res_len>0) {
		int copy_len;

		copy_len = res_len;
		if (copy_len > cmd_maxlen) {
			copy_len = cmd_maxlen;
		}
		if (cmd) {
			memcpy(cmd, buffer+1, copy_len);
		}
	}
	return res_len;
}

int gcn64_exchange(gcn64_hdl_t hdl, unsigned char *outcmd, int outlen, unsigned char *result, int result_max)
{
	int n;

	n = gcn64_send_cmd(hdl, outcmd, outlen);
	if (n<0) {
		fprintf(stderr, "Error sending command\n");
		return -1;
	}

	/* Answer to the command comes later. For now, this is polled, but in
	 * the future an interrupt-in transfer could be used. */
	do {
		n = gcn64_poll_result(hdl, result, result_max);
		if (n < 0) {
			fprintf(stderr, "Error\r\n");
			break;
		}
		if (n==0) {
//			printf("."); fflush(stdout);
		}

	} while (n==0);

	return n;
}

