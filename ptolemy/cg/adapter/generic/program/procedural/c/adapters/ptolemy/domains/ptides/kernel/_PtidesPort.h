/* In this file we have defined a struct PtidesPort
 *
 * @author : William Lucas
 */

#ifndef PTIDESPORT_H_
#define PTIDESPORT_H_

#include "_TypedIOPort.h"
#include "_PtidesDirector.h"

#define PTIDESPORT 11
#define IS_PTIDESPORT(p) ((p)->typePort%100 == 11)

struct PtidesPort {
	int typePort;

	struct Actor* container;

	bool _isInsideConnected;
	bool _isOutsideConnected;
	bool _isInput;
	bool _isOutput;
	bool _isMultiport;
	int _width;
	int _insideWidth;
	PblList* _farReceivers;
	PblList* _localReceivers;
	PblList* _localInsideReceivers;
	PblList* _insideReceivers;
	int _numberOfSinks;
	int _numberOfSources;

	void (*free)(struct PtidesPort*);

	void (*broadcast)(struct PtidesPort*, Token);
	void (*broadcast1)(struct PtidesPort*, Token*, int, int);
	PblList* (*deepGetReceivers)(struct PtidesPort*);
	Token (*get)(struct PtidesPort*, int);
	Token* (*get1)(struct PtidesPort*, int, int);
	int (*getChannelForReceiver)(struct PtidesPort*, struct Receiver*);
	Token (*getInside)(struct PtidesPort*, int);
	PblList* (*getInsideReceivers)(struct PtidesPort*);
	Time (*getModelTime)(struct PtidesPort*, int);
	PblList* (*getReceivers)(struct PtidesPort*);
	PblList* (*getRemoteReceivers)(struct PtidesPort*);
	int (*getWidth)(struct PtidesPort*);
	int (*getWidthInside)(struct PtidesPort*);
	bool (*hasRoom)(struct PtidesPort*, int);
	bool (*hasRoomInside)(struct PtidesPort*, int);
	bool (*hasToken)(struct PtidesPort*, int);
	bool (*hasToken1)(struct PtidesPort*, int, int);
	bool (*hasTokenInside)(struct PtidesPort*, int);
	bool (*isInput)(struct PtidesPort*);
	bool (*isMultiport)(struct PtidesPort*);
	bool (*isOutput)(struct PtidesPort*);
	bool (*isOutsideConnected)(struct PtidesPort*);
	int (*numberOfSinks)(struct PtidesPort*);
	int (*numberOfSources)(struct PtidesPort*);
	void (*send)(struct PtidesPort*, int, Token);
	void (*send1)(struct PtidesPort*, int, Token*, int);
	void (*sendInside)(struct PtidesPort*, int, Token);

#ifdef PTIDESDIRECTOR
	double delayOffset;
#endif

	char (*getType)(struct TypedIOPort*);
	char _type;

	// new members
	char* name;

	struct PtidesPort* _associatedPort;
	bool _settingAssociatedPort;

	void (*setAssociatedPort)(struct PtidesPort*, struct PtidesPort*);

	bool actuateAtEventTimestamp;
	Time deviceDelay;
	Time deviceDelayBound;
	bool isNetworkPort;
	Time networkDelayBound;
	Time platformDelayBound;
	Time sourcePlatformDelayBound;
	PblMap* _transmittedTokenTimestamps;
	PblMap* _transmittedTokenCnt;

	bool (*isActuatorPort)(struct PtidesPort*);
	bool (*isSensorPort)(struct PtidesPort*);
	bool (*isNetworkReceiverPort)(struct PtidesPort*);
	bool (*isNetworkTransmitterPort)(struct PtidesPort*);

	Time* (*_getTimeStampForToken)(struct PtidesPort*, Token);
};

struct PtidesPort* PtidesPort_New();
void PtidesPort_Init(struct PtidesPort* port);
void PtidesPort_New_Free(struct PtidesPort* port);

void PtidesPort_SetAssociatedPort(struct PtidesPort* port, struct PtidesPort* port1);
bool PtidesPort_IsActuatorPort(struct PtidesPort* port);
bool PtidesPort_IsSensorPort(struct PtidesPort* port);
bool PtidesPort_IsNetworkReceiverPort(struct PtidesPort* port);
bool PtidesPort_IsNetworkTransmitterPort(struct PtidesPort* port);
void PtidesPort_Send(struct PtidesPort* port, int channelIndex, Token token);

Time* PtidesPort__GetTimeStampForToken(struct PtidesPort* port, Token t);


#endif
