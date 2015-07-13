COMPONENT=NodeIfC
CFLAGS += -DCC2420_NO_ACKNOWLEDGEMENTS
CFLAGS += -DCC2420_NO_ADDRESS_RECOGNITION
CFLAGS += -DTASKLET_IS_TASK
CFLAGS += -I$(TOSDIR)/lib/printf

CFLAGS += -DCC2420_DEF_RFPOWER=31

TOSMAKE_PRE_EXE_DEPS += RssiMsg.py NodeIfMsg.py
TOSMAKE_CLEAN_EXTRA = RssiMsg.py NodeIfMsg.py *.pyc

RssiMsg.py: java/RssiMessages.h
	nescc-mig python -python-classname=RssiMsg $< RssiMsg -o $@

NodeIfMsg.py: NodeIfMessages.h
	nescc-mig python -python-classname=NodeIfMsg $< NodeIfMsg -o $@


include $(TINYOS_ROOT_DIR)/Makefile.include

