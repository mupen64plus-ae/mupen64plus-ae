#/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
# *   Mupen64plus - Makefile                                                *
# *   Mupen64Plus homepage: https://mupen64plus.org/                        *
# *   Copyright (C) 2009 Richard42                                          *
# *                                                                         *
# *   This program is free software; you can redistribute it and/or modify  *
# *   it under the terms of the GNU General Public License as published by  *
# *   the Free Software Foundation; either version 2 of the License, or     *
# *   (at your option) any later version.                                   *
# *                                                                         *
# *   This program is distributed in the hope that it will be useful,       *
# *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
# *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
# *   GNU General Public License for more details.                          *
# *                                                                         *
# *   You should have received a copy of the GNU General Public License     *
# *   along with this program; if not, write to the                         *
# *   Free Software Foundation, Inc.,                                       *
# *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.          *
# * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */
# Makefile for Mupen64plus-ui-console

# detect operation system
UNAME ?= $(shell uname -s)
OS := NONE
ifeq ("$(UNAME)","Linux")
  OS = LINUX
endif
ifeq ("$(UNAME)","linux")
  OS = LINUX
endif
ifneq ("$(filter GNU hurd,$(UNAME))","")
  OS = LINUX
endif
ifeq ("$(UNAME)","Darwin")
  OS = OSX
endif
ifeq ("$(UNAME)","FreeBSD")
  OS = FREEBSD
endif
ifeq ("$(UNAME)","OpenBSD")
  OS = FREEBSD
endif
ifneq ("$(filter GNU/kFreeBSD kfreebsd,$(UNAME))","")
  OS = LINUX
endif
ifeq ("$(patsubst MINGW%,MINGW,$(UNAME))","MINGW")
  OS = MINGW
endif
ifeq ("$(OS)","NONE")
  $(error OS type "$(UNAME)" not supported.  Please file bug report at 'https://github.com/mupen64plus/mupen64plus-core/issues')
endif

# detect system architecture, only if it matters for build flags
HOST_CPU ?= $(shell uname -m)
CPU := OTHER
ifneq ("$(filter x86_64 amd64,$(HOST_CPU))","")
  CPU := X86
  ifeq ("$(BITS)", "32")
    ARCH_DETECTED := 64BITS_32
  else
    ARCH_DETECTED := 64BITS
  endif
endif
ifneq ("$(filter pentium i%86,$(HOST_CPU))","")
  CPU := X86
  ARCH_DETECTED := 32BITS
endif

# base CFLAGS, LDLIBS, and LDFLAGS
OPTFLAGS ?= -O3 -flto
WARNFLAGS ?= -Wall

CFLAGS += $(OPTFLAGS) $(WARNFLAGS) -ffast-math -fno-strict-aliasing -I../../src
ifeq ($(OS), MINGW)
  CFLAGS += -lpthread
  LDLIBS += -lpthread
else
  ifneq ($(OS), OSX)
    CFLAGS += -pthread
    LDLIBS += -pthread
  endif
endif

# set special flags per-system
ifeq ($(OS), LINUX)
  LDLIBS += -ldl
endif
ifeq ($(OS), OSX)
  OSX_SDK_PATH = $(shell xcrun --sdk macosx --show-sdk-path)
  LDLIBS += -framework CoreFoundation

  ifeq ($(CPU), X86)
    ifeq ($(ARCH_DETECTED), 64BITS)
      CFLAGS += -pipe -arch x86_64 -mmacosx-version-min=10.6 -isysroot $(OSX_SDK_PATH)
    else
      CFLAGS += -pipe -mmmx -msse -fomit-frame-pointer -arch i686 -mmacosx-version-min=10.6 -isysroot $(OSX_SDK_PATH)
    endif
  endif
endif

# test for presence of SDL
ifeq ($(origin SDL_CFLAGS) $(origin SDL_LDLIBS), undefined undefined)
  SDL_CONFIG = $(CROSS_COMPILE)sdl2-config
  ifeq ($(shell which $(SDL_CONFIG) 2>/dev/null),)
    SDL_CONFIG = $(CROSS_COMPILE)sdl-config
    ifeq ($(shell which $(SDL_CONFIG) 2>/dev/null),)
      $(error No SDL development libraries found!)
    else
      $(warning Using SDL 1.2 libraries)
    endif
  endif
  SDL_CFLAGS  += $(shell $(SDL_CONFIG) --cflags)
  SDL_LDLIBS += $(shell $(SDL_CONFIG) --libs)
endif
CFLAGS += $(SDL_CFLAGS)
LDLIBS += $(SDL_LDLIBS)

ifeq ($(OS), MINGW)
  LDLIBS += -mconsole
endif

ifeq ($(BIG_ENDIAN), 1)
  CFLAGS += -DM64P_BIG_ENDIAN
endif

# tweak flags for 32-bit build on 64-bit system
ifeq ($(ARCH_DETECTED), 64BITS_32)
  ifeq ($(OS), FREEBSD)
    $(error Do not use the BITS=32 option with FreeBSD, use -m32 and -m elf_i386)
  endif
  CFLAGS += -m32
  LDFLAGS += -Wl,-m,elf_i386
endif

# set mupen64plus core API header path
ifneq ("$(APIDIR)","")
  CFLAGS += "-I$(APIDIR)"
else
  TRYDIR = ../../../mupen64plus-core/src/api
  ifneq ("$(wildcard $(TRYDIR)/m64p_types.h)","")
    CFLAGS += -I$(TRYDIR)
  else
    TRYDIR = /usr/local/include/mupen64plus
    ifneq ("$(wildcard $(TRYDIR)/m64p_types.h)","")
      CFLAGS += -I$(TRYDIR)
    else
      TRYDIR = /usr/include/mupen64plus
      ifneq ("$(wildcard $(TRYDIR)/m64p_types.h)","")
        CFLAGS += -I$(TRYDIR)
      else
        $(error Mupen64Plus API header files not found! Use makefile parameter APIDIR to force a location.)
      endif
    endif
  endif
endif

# reduced compile output when running make without V=1
ifneq ($(findstring $(MAKEFLAGS),s),s)
ifndef V
	Q_CC  = @echo '    CC  '$@;
	Q_LD  = @echo '    LD  '$@;
endif
endif

# set base program pointers and flags
CC        = $(CROSS_COMPILE)gcc
CXX       = $(CROSS_COMPILE)g++
RM       ?= rm -f
INSTALL  ?= install
MKDIR ?= mkdir -p
COMPILE.c = $(Q_CC)$(CC) $(CFLAGS) $(CPPFLAGS) $(TARGET_ARCH) -c
LINK.o = $(Q_LD)$(CC) $(CFLAGS) $(LDFLAGS) $(TARGET_ARCH)

ifeq ($(PIE), 1)
  CFLAGS += -fPIE
  LDFLAGS += -pie
else
  CFLAGS += -fno-PIE
  ifeq ($(CC),$(CROSS_COMPILE)gcc)
    # check if PIE is the default for the compiler
    PIE_DEFAULT = $(shell $(CC) -v 2>&1 | grep enable-default-pie)
    ifneq ($(PIE_DEFAULT),)
      LDFLAGS += -no-pie
    endif
  endif
endif

# set installation options
ifeq ($(PREFIX),)
  PREFIX := /usr/local
endif
ifeq ($(LIBDIR),)
  LIBDIR := $(PREFIX)/lib
endif
ifeq ($(BINDIR),)
  BINDIR := $(PREFIX)/bin
endif
ifeq ($(MANDIR),)
  MANDIR := $(PREFIX)/share/man
endif
ifeq ($(APPSDIR),)
  APPSDIR := $(PREFIX)/share/applications
endif
ifeq ($(ICONSDIR),)
  ICONSDIR := $(PREFIX)/share/icons/hicolor
endif

# set special flags for given Makefile parameters
ifeq ($(PLUGINDIR),)
  CFLAGS += '-DPLUGINDIR="$(LIBDIR)/mupen64plus"'
else
  CFLAGS += '-DPLUGINDIR="$(PLUGINDIR)"'
endif
# note: COREDIR _must_ end in a slash if you want it to work; not necessary for the others
ifeq ($(COREDIR),)
  CFLAGS += '-DCOREDIR="$(LIBDIR)/"'
else
  CFLAGS += '-DCOREDIR="$(COREDIR)"'
endif
ifneq ($(SHAREDIR),)
  CFLAGS += '-DSHAREDIR="$(SHAREDIR)"'
endif
ifeq ($(DEBUG),1)
  CFLAGS += -g
  INSTALL_STRIP_FLAG ?= 
else
  INSTALL_STRIP_FLAG ?= -s
endif

SRCDIR = ../../src
OBJDIR = _obj$(POSTFIX)

ifeq ("$(OS)","MINGW")
  EXEEXT = .exe
else
  EXEEXT =
endif

# list of source files to compile
SOURCE = \
	$(SRCDIR)/cheat.c \
	$(SRCDIR)/compare_core.c \
	$(SRCDIR)/core_interface.c \
	$(SRCDIR)/debugger.c \
	$(SRCDIR)/main.c \
	$(SRCDIR)/plugin.c

ifeq ($(OS), MINGW)
SOURCE += \
	$(SRCDIR)/osal_dynamiclib_win32.c \
	$(SRCDIR)/osal_files_win32.c
else
SOURCE += \
	$(SRCDIR)/osal_dynamiclib_unix.c \
	$(SRCDIR)/osal_files_unix.c
endif

# generate a list of object files build, make a temporary directory for them
OBJECTS := $(patsubst $(SRCDIR)/%.c, $(OBJDIR)/%.o, $(filter %.c, $(SOURCE)))
OBJDIRS = $(dir $(OBJECTS))
$(shell $(MKDIR) $(OBJDIRS))

# build targets
TARGET = mupen64plus$(POSTFIX)$(EXEEXT)

targets:
	@echo "Mupen64Plus-ui-console makefile."
	@echo "  Targets:"
	@echo "    all            == Build Mupen64Plus console front-end application"
	@echo "    clean          == remove object files and build products"
	@echo "    rebuild        == clean and re-build all"
	@echo "    install        == Install Mupen64Plus console front-end application"
	@echo "    uninstall      == Uninstall Mupen64Plus console front-end application"
	@echo "  Options:"
	@echo "    COREDIR=path   == default path to search for Mupen64Plus Core (must end with slash)"
	@echo "    PLUGINDIR=path == default path to search for plugins"
	@echo "    SHAREDIR=path  == default path to search for shared data files"
	@echo "    APIDIR=path    == path to find Mupen64Plus Core headers"
	@echo "    OPTFLAGS=flags == compiler optimization (default: -O3 -flto)"
	@echo "    WARNFLAGS=flag == compiler warning levels (default: -Wall)"
	@echo "    PIE=(1|0)      == Force enable/disable of position independent executables"
	@echo "    POSTFIX=name  == String added to the name of the the build (default: '')"
	@echo "  Install Options:"
	@echo "    PREFIX=path    == install/uninstall prefix (default: /usr/local/)"
	@echo "    BINDIR=path    == path to install mupen64plus binary (default: PREFIX/bin/)"
	@echo "    MANDIR=path    == path to install mupen64plus manual page (default: PREFIX/share/man)"
	@echo "    APPSDIR=path   == path to install desktop file (default: PREFIX/share/applications)"
	@echo "    ICONSDIR=path  == path to install icon files (default: PREFIX/share/icons/hicolor)"
	@echo "    DESTDIR=path   == path to prepend to all installation paths (only for packagers)"
	@echo "  Debugging Options:"
	@echo "    DEBUG=1        == add debugging symbols to application binary"
	@echo "    V=1            == show verbose compiler output"

all: $(TARGET)

clean:
	$(RM) -r $(OBJDIR) $(TARGET)

rebuild: clean all

install: $(TARGET)
	$(INSTALL) -d "$(DESTDIR)$(BINDIR)"
	$(INSTALL) -m 0755 $(INSTALL_STRIP_FLAG) $(TARGET) "$(DESTDIR)$(BINDIR)"
	$(INSTALL) -d "$(DESTDIR)$(MANDIR)/man6"
	$(INSTALL) -m 0644 ../../doc/mupen64plus.6 "$(DESTDIR)$(MANDIR)/man6"
	$(INSTALL) -d "$(DESTDIR)$(APPSDIR)"
	$(INSTALL) -m 0644 ../../data/mupen64plus.desktop "$(DESTDIR)$(APPSDIR)"
	$(INSTALL) -d "$(DESTDIR)$(ICONSDIR)/48x48/apps"
	$(INSTALL) -m 0644 ../../data/icons/48x48/apps/mupen64plus.png "$(DESTDIR)$(ICONSDIR)/48x48/apps"
	$(INSTALL) -d "$(DESTDIR)$(ICONSDIR)/scalable/apps"
	$(INSTALL) -m 0644 ../../data/icons/scalable/apps/mupen64plus.svg "$(DESTDIR)$(ICONSDIR)/scalable/apps"


uninstall:
	$(RM) "$(DESTDIR)$(BINDIR)/$(TARGET)" "$(DESTDIR)$(MANDIR)/man6/mupen64plus.6"
	$(RM) "$(DESTDIR)$(APPSDIR)/mupen64plus.desktop"
	$(RM) "$(DESTDIR)$(ICONSDIR)/48x48/apps/mupen64plus.png"
	$(RM) "$(DESTDIR)$(ICONSDIR)/scalable/apps/mupen64plus.svg"


# build dependency files
CFLAGS += -MD -MP
-include $(OBJECTS:.o=.d)

# standard build rules
$(OBJDIR)/%.o: $(SRCDIR)/%.c
	$(COMPILE.c) -o $@ $<

$(TARGET): $(OBJECTS)
	$(LINK.o) $^ $(LOADLIBES) $(LDLIBS) -o $@

.PHONY: all clean install uninstall targets
