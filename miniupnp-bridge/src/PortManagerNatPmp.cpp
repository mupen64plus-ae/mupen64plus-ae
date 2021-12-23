#include <algorithm>
#include <cstdlib>
#include <cstring>
#include <string>
#include <thread>
#include <mutex>
#include <arpa/inet.h>

#include <android/log.h>

#include "PortManagerNatPmp.h"

PortManagerNatPmp g_PortManagerNatPmp;

PortManagerNatPmp::PortManagerNatPmp()
{

}

PortManagerNatPmp::~PortManagerNatPmp()
{
	Clear();
	Terminate();
}

void PortManagerNatPmp::Terminate()
{
    __android_log_write(ANDROID_LOG_VERBOSE, "miniupnp-bridge", "PortManagerNatPmp::Terminate()");


	closenatpmp(&m_natpmp);

	m_portList.clear();
}

bool PortManagerNatPmp::Initialize(int gatewayIp)
{
	__android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "PortManagerNatPmp::Initialize(%d)", gatewayIp);

	natpmpresp_t response;

	int r;

	bool initFinished = false;
	int retCode = initnatpmp(&m_natpmp, 1, gatewayIp);

	if( retCode < 0) {
		__android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "PortManagerNatPmp::Initialize failure! code=%d", retCode);
		m_currentNatPmpState = Serror;
	} else {
		m_currentNatPmpState = Ssendpub;
	}

	while(!initFinished) {
		switch(m_currentNatPmpState) {
			case Ssendpub:
				if(sendpublicaddressrequest(&m_natpmp) < 0) {
					m_currentNatPmpState = Serror;
					__android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "Failure to send address request");
				} else {
                    __android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "Success in sending address request");

                    m_currentNatPmpState = Srecvpub;
				}
				break;
			case Srecvpub:
				r = readnatpmpresponseorretry(&m_natpmp, &response);
				if(r<0 && r!=NATPMP_TRYAGAIN) {
					m_currentNatPmpState = Serror;
					__android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "Failure to read address request");

				} else if(r!=NATPMP_TRYAGAIN) {
					m_publicAddress = response.pnu.publicaddress.addr;
					m_currentNatPmpState = Ssendmap;

					char str[INET_ADDRSTRLEN];
					inet_ntop(AF_INET, &m_publicAddress, str, INET_ADDRSTRLEN);

					__android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "Public address: %s", str);
				}
				break;
			default:
				initFinished = true;
		}
	}

	m_initiliazed = m_currentNatPmpState != Serror;

	__android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "NATPMP init success=%d",  m_initiliazed);

	return m_initiliazed;
}

bool PortManagerNatPmp::Add(const char* protocol, unsigned short port, unsigned short intport)
{
    __android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "PortManagerNatPmp::Add(%s, %d, %d)", protocol, port, intport);

    if (!m_initiliazed) {
		__android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "PortManagerNatPmp::Add, library not initialized!");
		return false;
    }

	natpmpresp_t response;

	int protocolInt = strcmp(protocol, "TCP") == 0 ? NATPMP_PROTOCOL_TCP : NATPMP_PROTOCOL_UDP;

	int r;

	bool initFinished = false;

	while(!initFinished) {

	    static NatpmpState state = Serror;
	    if (state != m_currentNatPmpState) {
            __android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge",
                                "add mapping request  (%s, %d, %d) : state=%d", protocol, port,
                                intport, m_currentNatPmpState);
            state = m_currentNatPmpState;
        }
        switch(m_currentNatPmpState) {
			case Ssendmap:
				if(sendnewportmappingrequest(&m_natpmp, protocolInt, intport, port, m_leaseDuration) < 0) {
					m_currentNatPmpState = Serror;
					__android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "PortManagerNatPmp failure on send (%s, %d, %d)", protocol, port, intport);
				} else {
                    __android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "Sent mapping request  (%s, %d, %d)", protocol, port, intport);

					m_currentNatPmpState = Srecvmap;
				}
				break;
			case Srecvmap:
				r = readnatpmpresponseorretry(&m_natpmp, &response);
				if(r < 0 && r != NATPMP_TRYAGAIN) {
					m_currentNatPmpState = Serror;
					__android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "PortManagerNatPmp failure on read (%s, %d, %d)", protocol, port, intport);
				} else if(r != NATPMP_TRYAGAIN) {

                    __android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "read mapping request  (%s, %d, %d)", protocol, port, intport);

					InternalPort internalPortKey;
					internalPortKey.internalPort = response.pnu.newportmapping.privateport;
					internalPortKey.protocol = protocolInt;
					InternalPortData portData;
					portData.externalPort = response.pnu.newportmapping.mappedpublicport;
					portData.leaseDuration = response.pnu.newportmapping.lifetime;
					m_portList[internalPortKey] = portData;
					m_currentNatPmpState = Sdone;
				}
				break;
			default:
				initFinished = true;
		}
	}

    m_currentNatPmpState = Ssendmap;

	return m_currentNatPmpState != Serror;
}

bool PortManagerNatPmp::Remove(const char* protocol, unsigned short port)
{
    __android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "PortManagerNatPmp::Remove(%s, %d)", protocol, port);

	if (!m_initiliazed) {
		__android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "PortManagerNatPmp::Remove, library not initialized!");
		return false;
	}

	natpmpresp_t response;

	int protocolInt = strcmp(protocol, "TCP") == 0 ? NATPMP_PROTOCOL_TCP : NATPMP_PROTOCOL_UDP;

	InternalPort internalPortKey;
	internalPortKey.internalPort = port;
	internalPortKey.protocol = protocolInt;
	InternalPortData portData = m_portList[internalPortKey];

	int r;

	bool initFinished = false;

	while(!initFinished) {
		switch(m_currentNatPmpState) {
			case Ssendmap:
				// To unmap, set the lease duration to zero
				if(sendnewportmappingrequest(&m_natpmp, protocolInt, port, portData.externalPort, 0) < 0)
					m_currentNatPmpState = Serror;
				else
					m_currentNatPmpState = Srecvmap;
				break;
			case Srecvmap:
				r = readnatpmpresponseorretry(&m_natpmp, &response);
				if(r < 0 && r != NATPMP_TRYAGAIN)
					m_currentNatPmpState = Serror;
				else if(r != NATPMP_TRYAGAIN) {
					m_portList.erase(internalPortKey);
					m_currentNatPmpState = Sdone;
				}
				break;
			default:
				initFinished = true;
		}
	}

    m_currentNatPmpState = Ssendmap;

	return m_currentNatPmpState != Serror;
}

void PortManagerNatPmp::Clear()
{
	if (!m_initiliazed) {
		__android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "PortManagerNatPmp::Clear, library not initialized!");
		return;
	}

	auto mapCopy = m_portList;
	for (auto portData : mapCopy) {
		uint16_t internalPort = portData.first.internalPort;
		const char* protocol = portData.first.protocol == NATPMP_PROTOCOL_TCP ? "TCP" : "UDP";
		Remove(protocol, internalPort);
	}
}

extern "C" __attribute__((visibility("default"))) void NATPMP_Init(int gatewayIp)
{
	g_PortManagerNatPmp.Initialize(gatewayIp);
}

extern "C" __attribute__((visibility("default"))) void NATPMP_Shutdown()
{
	g_PortManagerNatPmp.Clear();
	g_PortManagerNatPmp.Terminate();
}

extern "C" __attribute__((visibility("default"))) bool NATPMP_Add(const char* protocol, int port, int intport)
{
	__android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "NATPMP_Add(%s, %d)", protocol, port);
	return g_PortManagerNatPmp.Add(protocol, port, intport);
}

extern "C" __attribute__((visibility("default"))) bool NATPMP_Remove(const char* protocol, int port)
{
	__android_log_print(ANDROID_LOG_INFO, "miniupnp-bridge", "NATPMP_Remove(%s, %d)", protocol, port);
	return g_PortManagerNatPmp.Remove(protocol, port);
}

