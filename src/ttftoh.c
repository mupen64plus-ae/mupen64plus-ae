/***************************************************************************
                                 arial.ttf.h
                             -------------------
 Copyright (C) 2002 Gregor Anich (blight)
 ***************************************************************************/

/***************************************************************************
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *                                                                         *
 ***************************************************************************/

#include <errno.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <unistd.h>
#include <sys/types.h>
#include <sys/stat.h>

int
main( int argc, char *argv[] )
{
    FILE *f, *fo;
    char fnameo[1024];
    int i, c, fsize;
    struct stat sb;

    if( argc != 2 )
    {
        fprintf( stderr, "Usage: %s [font]\n", argv[0] );
        return( EXIT_FAILURE );
    }

    if( stat( argv[1], &sb ) == -1 )
    {
        fprintf( stderr, "Couldn't read filesize of '%s': %s\n", argv[1], strerror( errno ) );
        return( EXIT_FAILURE );
    }
    fsize = sb.st_size;

    f = fopen( argv[1], "rb" );
    if( f == NULL )
    {
        fprintf( stderr, "Couldn't open file '%s' for reading: %s\n", argv[1], strerror( errno ) );
        return( EXIT_FAILURE );
    }

    sprintf( fnameo, "%s.h", argv[1] ); // possible overflow
    fo = fopen( fnameo, "wb" );
    if( fo == NULL )
    {
        fprintf( stderr, "Couldn't open file '%s' for writing: %s\n", fnameo, strerror( errno ) );
        fclose( f );
        return( EXIT_FAILURE );
    }

    fprintf( fo, "struct {\n  int size;\n  const char *data;\n} arial = {\n  %d,\n", fsize );

    i = 0;
    while( !feof( f ) )
    {
        c = fgetc( f );
        if( c == EOF )
            break;
        if( !(i % 20) )
            fprintf( fo, "  \"" );
        fprintf( fo, "\\x%02X", c );
        i++;
        if( !(i % 20) )
            fprintf( fo, "\"\n" );
    }
    if( i % 20 )
        fprintf( fo, "\"\n" );
    fprintf( fo, "};\n" );

    fclose( f );
    fclose( fo );

    return( EXIT_SUCCESS );
}

