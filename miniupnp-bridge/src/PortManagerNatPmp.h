#pragma once


#include <libnatpmp/natpmp.h>

#include <string>
#include <map>

#define IP_PROTOCOL_TCP	"TCP"
#define IP_PROTOCOL_UDP	"UDP"

enum NatpmpState { Sinit=0, Ssendpub, Srecvpub, Ssendmap, Srecvmap, Sdone, Serror=1000 };

struct UPNPUrls;
struct IGDdatas;

struct PortMap {
	bool taken;
	std::string protocol;
	std::string extPort_str;
	std::string intPort_str;
	std::string lanip;
	std::string remoteHost;
	std::string desc;
	std::string duration;
	std::string enabled;
};

class PortManagerNatPmp {
public:
	PortManagerNatPmp();
	~PortManagerNatPmp();

	// Initialize NAT PMP
	bool Initialize(int gatewayIp);

	// Uninitialize/Reset the state
	void Terminate();

	// Add a port & protocol (TCP, UDP or vendor-defined) to map for forwarding (intport = 0 : same as [external] port)
	bool Add(const char* protocol, unsigned short port, unsigned short intport = 0);

	// Remove a port mapping (external port)
	bool Remove(const char* protocol, unsigned short port);

	// Removes any lingering mapped ports created by the app
	void Clear();

private:
	struct InternalPort
	{
		uint16_t internalPort;
		int protocol;

		bool operator < (const InternalPort& other) const
		{
			return std::tie(internalPort, protocol) < std::tie(other.internalPort, other.protocol);
		}
	};

	struct InternalPortData
	{
		uint16_t externalPort;
		int leaseDuration;
	};

	// Key is internal port, value is InternalPortData
	std::map<InternalPort, InternalPortData> m_portList;

	NatpmpState m_currentNatPmpState = Sinit;

	in_addr m_publicAddress;

	int m_leaseDuration = 43200;

	natpmp_t m_natpmp;

	bool m_initiliazed = false;
};

extern PortManagerNatPmp g_PortManagerNatPmp;
