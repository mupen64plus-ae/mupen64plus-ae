/* * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * *
 *   Mupen64plus - cheat.c                                                 *
 *   Mupen64Plus homepage: http://code.google.com/p/mupen64plus/           *
 *   Copyright (C) 2009 Richard Goedeken                                   *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 *   This program is distributed in the hope that it will be useful,       *
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of        *
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the         *
 *   GNU General Public License for more details.                          *
 *                                                                         *
 *   You should have received a copy of the GNU General Public License     *
 *   along with this program; if not, write to the                         *
 *   Free Software Foundation, Inc.,                                       *
 *   51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA.          *
 * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * * */

#include <stdlib.h>
#include <string.h>
#include <stdio.h>

#include "m64p_types.h"
#include "cheat.h"
#include "core_interface.h"

/* local definitions */
#define DATABASE_FILENAME "mupen64plus.cht"

typedef struct _sCheatInfo {
  int                 Number;
  const char         *Name;
  const char         *Description;
  const char         *Codes;
  struct _sCheatInfo *Next;
  } sCheatInfo;

/* local variables */
static m64p_rom_header  l_RomHeader;

static char            *l_IniText = NULL;
static const char      *l_CheatGameName = NULL;
static sCheatInfo      *l_CheatList = NULL;
static int              l_CheatCodesFound = 0;
static int              l_RomFound = 0;

/*********************************************************************************************************
 *  Static (Local) functions
 */

static int isSpace(char ch)
{
    return (ch == ' ' || ch == '\t' || ch == '\r' || ch == '\n');
}

static void CheatNewCode(char *CheatName, int CheatNum, char *CheatCodes)
{
    /* allocate memory for a new sCheatInfo struct */
    sCheatInfo *pNew = (sCheatInfo *) malloc(sizeof(sCheatInfo));
    if (pNew == NULL) return;

    /* fill in the data members */
    pNew->Number = CheatNum;
    pNew->Name = CheatName;
    pNew->Description = NULL;
    pNew->Codes = CheatCodes;
    pNew->Next = NULL;

    l_CheatCodesFound++;

    /* stick it at the end of the list */
    if (l_CheatList == NULL)
    {
        l_CheatList = pNew;
        return;
    }
    sCheatInfo *pLast = l_CheatList;
    while (pLast->Next != NULL) pLast = pLast->Next;
    pLast->Next = pNew;
}

static sCheatInfo *CheatFindCode(int Number)
{
    sCheatInfo *pCur = l_CheatList;
    while (pCur != NULL)
    {
        if (pCur->Number == Number) break;
        pCur = pCur->Next;
    }
    return pCur;
}

/*
 * Read and parse the Cheat DATABASE (PJ64), and load up any cheat codes found for the specified ROM section
 */
void CheatParseIni(const char *RomSection)
{
    const char *romdbpath = ConfigGetSharedDataFilepath(DATABASE_FILENAME);
    if (romdbpath == NULL)
    {
        printf("UI-Console: Cheat code database file '%s' not found.\n", DATABASE_FILENAME);
        return;
    }

    /* read the INI file into a new buffer */
    FILE *fPtr = NULL;
    fPtr = fopen(romdbpath, "rb");
    if (fPtr == NULL)
    {   
        printf("UI-Console: Couldn't open cheat code database file '%s'.\n", romdbpath);
        return;
    }
    fseek(fPtr, 0L, SEEK_END);
    long IniLength = ftell(fPtr);
    fseek(fPtr, 0L, SEEK_SET);
    l_IniText = (char *) malloc(IniLength + 1);
    if (l_IniText == NULL)
    {
        printf("UI-Console: Couldn't allocate %li bytes of memory to read cheat ini file.\n", IniLength);
        fclose(fPtr);
        return;
    }
    if (fread(l_IniText, 1, IniLength, fPtr) != IniLength)
    {
        printf("UI-Console: Couldn't read %li bytes from cheat ini file.\n", IniLength);
        free(l_IniText);
        l_IniText = NULL;
        fclose(fPtr);
        return;
    }
    fclose(fPtr);
    l_IniText[IniLength] = 0; /* null-terminate the text data */

    /* parse lines from cheat database */
    char *curline = NULL;
    char *nextline = l_IniText;
    while(nextline != NULL && *nextline != 0)
    {
        curline = nextline;
        /* get pointer to next line and NULL-terminate the current line */
        nextline = strchr(curline, '\n');
        if (nextline != NULL)
        {
            *nextline = 0;
            nextline++;
        }

        /* remove leading and trailing white space */
        while(isSpace(*curline)) curline++;
        char *endptr = curline + strlen(curline) - 1;
        while(isSpace(*endptr)) *endptr-- = 0;

        /* handle beginning of new rom section */
        if (*curline == '[' && *endptr == ']')
        {
            /* if we have already found cheats for the given ROM file, then exit upon encountering a new ROM section */
            if (l_RomFound)
                return;
            /* else see if this Rom Section matches */
            curline++;
            *endptr-- = 0;
            if (strcmp(curline, RomSection) == 0)
                l_RomFound = 1;
            continue;
        }

        /* if we haven't found the specified ROM section, then continue looking */
        if (!l_RomFound)
            continue;

        /* skip over any comments or blank lines */
        if (curline[0] == '/' && curline[1] == '/')
            continue;
        if (strlen(curline) == 0)
            continue;

        /* Handle the game's name in the cheat file */
        if (strncmp(curline, "Name=", 5) == 0)
        {
            l_CheatGameName = curline + 5;
            continue;
        }
        /* Handle new cheat codes */
        char lineextra[64]; /* this isn't used but is needed because sscanf sucks */
        int CheatNum;
        if (sscanf(curline, "Cheat%i = \"%32s", &CheatNum, lineextra) == 2)
        {
            char *CheatName = strchr(curline, '"') + 1;
            /* NULL-terminate the cheat code's name and get a pointer to the start of the codes */
            char *CheatCodes = strchr(CheatName, '"');
            if (CheatCodes == NULL) continue;
            *CheatCodes++ = 0;
            CheatCodes = strchr(CheatCodes, ',');
            if (CheatCodes == NULL) continue;
            CheatCodes++;
            /* If this is a cheat code with options, just skip it; too complicated for command-line UI */
            if (strchr(CheatCodes, '?') != NULL)
                continue;
            /* create a new cheat code in our list */
            CheatNewCode(CheatName, CheatNum, CheatCodes);
            continue;
        }
        /* Handle descriptions for cheat codes */
        if (sscanf(curline, "Cheat%i_N =%32s", &CheatNum, lineextra) == 2)
        {
            char *CheatDesc = strchr(curline, '=') + 1;
            sCheatInfo *pCheat = CheatFindCode(CheatNum);
            if (pCheat != NULL)
                pCheat->Description = CheatDesc;
            continue;
        }
        /* Handle options for cheat codes */
        if (sscanf(curline, "Cheat%i_O =%32s", &CheatNum, lineextra) == 2)
        {
            /* just skip it, options are too complicated for command-line UI */
            continue;
        }
        /* otherwise we don't know what this line is */
        printf("UI-Console Warning: unrecognized line in cheat ini file: '%s'\n", curline);
    }

}

static void CheatActivate(sCheatInfo *pCheat)
{
    m64p_cheat_code CodeArray[32];
    const char *pCodes = pCheat->Codes;
    int NumCodes = 0;

    while (pCodes != NULL && *pCodes != 0 && NumCodes < 32) /* I'm pretty sure none of the cheats contain 30 codes or more */
    {
        unsigned int address;
        int value;
        if (sscanf(pCodes, "%x %x", &address, &value) != 2)
        {
            printf("UI-Console Error: reading hex values in cheat code %i (%s)\n", pCheat->Number, pCodes);
            return;
        }
        CodeArray[NumCodes].address = address;
        CodeArray[NumCodes].value = value;
        NumCodes++;
        pCodes = strchr(pCodes, ',');
        if (pCodes != NULL) pCodes++;
    }

    if (CoreAddCheat(pCheat->Name, CodeArray, NumCodes) != M64ERR_SUCCESS)
    {
        printf("UI-Console Warning: CoreAddCheat() failed for cheat code %i (%s)\n", pCheat->Number, pCheat->Name);
        return;
    }

    printf("UI-Console: activated cheat code %i: %s\n", pCheat->Number, pCheat->Name);
}

static void CheatFreeAll(void)
{
    if (l_IniText != NULL)
        free(l_IniText);
    l_IniText = NULL;

    sCheatInfo *pCur = l_CheatList;
    while (pCur != NULL)
    {
        sCheatInfo *pNext = pCur->Next;
        free(pCur);
        pCur = pNext;
    }

    l_CheatList = NULL;
}

/*********************************************************************************************************
* global functions
*/

void CheatStart(eCheatMode CheatMode, int *CheatNumList, int CheatListLength)
{
    /* if cheat codes are disabled, then we don't have to do anything */
    if (CheatMode == CHEAT_DISABLE || (CheatMode == CHEAT_LIST && CheatListLength < 1))
    {
        printf("UI-Console: Cheat codes disabled.\n");
        return;
    }

    /* get the ROM header for the currently loaded ROM image from the core */
    if ((*CoreDoCommand)(M64CMD_ROM_GET_HEADER, sizeof(l_RomHeader), &l_RomHeader) != M64ERR_SUCCESS)
    {
        printf("UI-Console: couldn't get ROM header information from core library\n");
        return;
    }

    /* generate section name from ROM's CRC and country code */
    char RomSection[24];
    sprintf(RomSection, "%X-%X-C:%X", sl(l_RomHeader.CRC1), sl(l_RomHeader.CRC2), l_RomHeader.Country_code & 0xff);

    /* parse through the cheat INI file and load up any cheat codes found for this ROM */
    CheatParseIni(RomSection);
    if (!l_RomFound || l_CheatCodesFound == 0)
    {
        printf("UI-Console: no cheat codes found for ROM image '%.20s'\n", l_RomHeader.Name);
        CheatFreeAll();
        return;
    }

    /* handle the list command */
    if (CheatMode == CHEAT_SHOW_LIST)
    {
        printf("UI-Console: %i cheat code(s) found for ROM '%s'\n", l_CheatCodesFound, l_CheatGameName);
        sCheatInfo *pCur = l_CheatList;
        while (pCur != NULL)
        {
            if (pCur->Description == NULL)
                printf("   %i: %s\n", pCur->Number, pCur->Name);
            else
                printf("   %i: %s (%s)\n", pCur->Number, pCur->Name, pCur->Description);
            pCur = pCur->Next;
        }
        CheatFreeAll();
        return;
    }

    /* handle all cheats enabled mode */
    if (CheatMode == CHEAT_ALL)
    {
        sCheatInfo *pCur = l_CheatList;
        while (pCur != NULL)
        {
            CheatActivate(pCur);
            pCur = pCur->Next;
        }
        CheatFreeAll();
        return;
    }

    /* handle list of cheats enabled mode */
    if (CheatMode == CHEAT_LIST)
    {
        int i;
        for (i = 0; i < CheatListLength; i++)
        {
            sCheatInfo *pCheat = CheatFindCode(CheatNumList[i]);
            if (pCheat == NULL)
                printf("UI-Console Warning: invalid cheat code number %i\n", CheatNumList[i]);
            else
                CheatActivate(pCheat);
        }
        CheatFreeAll();
        return;
    }

    /* otherwise the mode is invalid */
    printf("UI-Console: internal error; invalid CheatMode in CheatStart()\n");
    return;
}


