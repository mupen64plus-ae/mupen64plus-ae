# set mupen64plus core API header path
ifneq ("$(APIDIR)","")
  MPAPI_PATH = $(APIDIR)
else
  TRYDIR = ../mupen64plus-core/src/api
  ifneq ("$(wildcard $(TRYDIR)/m64p_types.h)","")
    MPAPI_PATH = $(shell realpath $(TRYDIR))
  else
    TRYDIR = /usr/local/include/mupen64plus
    ifneq ("$(wildcard $(TRYDIR)/m64p_types.h)","")
      MPAPI_PATH = $(TRYDIR)
    else
      TRYDIR = /usr/include/mupen64plus
      ifneq ("$(wildcard $(TRYDIR)/m64p_types.h)","")
        MPAPI_PATH = $(TRYDIR)
      else
        $(error Mupen64Plus API header files not found! Use makefile parameter APIDIR to force a location.)
      endif
    endif
  endif
endif

export MPAPI_PATH

all: glitch64 glide64

glitch64:
	$(MAKE) $(MFLAGS) -C Glitch64 -f Makefile.gcc
	cp Glitch64/glide3x.so Glide64/lib/
glide64:
	$(MAKE) $(MFLAGS) -C Glide64 -f Makefile.gcc

clean:
	$(MAKE) $(MFLAGS) -C Glitch64 -f Makefile.gcc clean
	$(MAKE) $(MFLAGS) -C Glide64 -f Makefile.gcc clean
