#***************************************************************************
#                           blight_input Makefile
#                            -------------------
# Copyright (C) 2007-2008 Richard Goedeken (Richard42)
#***************************************************************************

#***************************************************************************
#*                                                                         *
#*   This program is free software; you can redistribute it and/or modify  *
#*   it under the terms of the GNU General Public License as published by  *
#*   the Free Software Foundation; either version 2 of the License, or     *
#*   (at your option) any later version.                                   *
#*                                                                         *
#***************************************************************************

# include pre-make file with a bunch of definitions
ifeq ($(WIN32),1)
  include ../pre.mk.win32
else
  include ../pre.mk
endif

-include ../configure.gen

# local CFLAGS, LIBS, and LDFLAGS
SDLTTF_LIBS	= -lSDL_ttf
CFLAGS += -DGUI_SDL -DPIC

ifneq ($(OS), WINDOWS)
  CFLAGS += -fpic
else
  SDLTTF_LIBS += -L$(SDL_TTF_DIR)\lib
endif

ifeq ($(PLUGINDBG), 1)
  CFLAGS += -D_DEBUG
endif
# list of object files to generate
OBJ_BLIGHT = \
	plugin.o \
	arial.ttf.o \
	configdialog_sdl.o \
	pad.o

# build targets
all: blight_input.$(SO_EXTENSION)

clean:
	rm -f *.o *.$(SO_EXTENSION) ttftoh arial.ttf.c

# build rules
.c.o:
	$(CC) -o $@ $(CFLAGS) $(SDL_FLAGS) $(FREETYPE_FLAGS) -c $<

blight_input.$(SO_EXTENSION): $(OBJ_BLIGHT)
	$(CC) -o $@ -shared $^ $(LDFLAGS) $(PLUGIN_LDFLAGS) $(SDL_LIBS) $(SDLTTF_LIBS) $(FREETYPE_LIBS)
ifneq ($(OS), WINDOWS)
	$(STRIP) $@
endif

configdialog_sdl.o: configdialog_sdl.c
	$(CC) -o $@ $(CFLAGS) $(SDL_FLAGS) -c $<

plugin.o: plugin.c
	$(CC) -o $@ $(CFLAGS) $(SDL_FLAGS) -c $<

arial.ttf.c: ttftoh
	./ttftoh ./font/arial.ttf
ifneq ($(OS), WINDOWS)
	$(MV) ./font/arial.ttf.h ./arial.ttf.c
else
# no idea why, but move fails.
	copy font\arial.ttf.h arial.ttf.c
endif

ttftoh: ttftoh.o
	$(CC) $^ -o $@
ifneq ($(OS), WINDOWS)
	$(STRIP) $@
endif

ttftoh.o: ttftoh.c
	$(CC) -o $@ -O3 -c $<

