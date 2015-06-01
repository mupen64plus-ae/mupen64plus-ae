include $(CLEAR_VARS)
LOCAL_PATH := $(JNI_LOCAL_PATH)
SRCDIR := freetype

# compile in ARM mode, since the glyph loader/renderer is a hotspot
# when loading complex pages in the browser
#
LOCAL_ARM_MODE := arm
LOCAL_MODULE:= freetype
LOCAL_SHARED_LIBRARIES := png

LOCAL_C_INCLUDES +=   \
    $(LOCAL_PATH)/$(SRCDIR)/builds  \
    $(LOCAL_PATH)/$(SRCDIR)/include \
    $(PNG_INCLUDES)                 \
	
LOCAL_SRC_FILES:= \
	$(SRCDIR)/src/base/ftbbox.c \
	$(SRCDIR)/src/base/ftbitmap.c \
	$(SRCDIR)/src/base/ftfstype.c \
	$(SRCDIR)/src/base/ftglyph.c \
	$(SRCDIR)/src/base/ftlcdfil.c \
	$(SRCDIR)/src/base/ftstroke.c \
	$(SRCDIR)/src/base/fttype1.c \
	$(SRCDIR)/src/base/ftxf86.c \
	$(SRCDIR)/src/base/ftbase.c \
	$(SRCDIR)/src/base/ftsystem.c \
	$(SRCDIR)/src/base/ftinit.c \
	$(SRCDIR)/src/base/ftgasp.c \
	$(SRCDIR)/src/gzip/ftgzip.c \
	$(SRCDIR)/src/raster/raster.c \
	$(SRCDIR)/src/sfnt/sfnt.c \
	$(SRCDIR)/src/smooth/smooth.c \
	$(SRCDIR)/src/autofit/autofit.c \
	$(SRCDIR)/src/truetype/truetype.c \
	$(SRCDIR)/src/cff/cff.c \
	$(SRCDIR)/src/psnames/psnames.c \
	$(SRCDIR)/src/pshinter/pshinter.c

LOCAL_CFLAGS += -W -Wall
LOCAL_CFLAGS += -fPIC -DPIC
LOCAL_CFLAGS += "-DDARWIN_NO_CARBON"
LOCAL_CFLAGS += "-DFT2_BUILD_LIBRARY"

# the following is for testing only, and should not be used in final builds
# of the product
#LOCAL_CFLAGS += "-DTT_CONFIG_OPTION_BYTECODE_INTERPRETER"

LOCAL_CFLAGS += -O2

include $(BUILD_SHARED_LIBRARY)
