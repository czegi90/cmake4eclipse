/*******************************************************************************
 * Copyright (c) 2014 Martin Weber.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *      Martin Weber - Initial implementation
 *******************************************************************************/
package de.marw.cdt.cmake.core.cmakecache;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import de.marw.cdt.cmake.core.cmakecache.CMakeCacheFileParser.EntryFilter;

/**
 * Tests for {@link de.marw.cdt.cmake.core.cmakecache.CMakeCacheFileParser}.
 *
 * @author Martin Weber
 */
public class CMakeCacheFileParserTest {

  private CMakeCacheFileParser testee;

  /**
   * @throws java.lang.Exception
   */
  @Before
  public void setUp() throws Exception {
    testee = new CMakeCacheFileParser();
  }

  /**
   * Test method for
   * {@link de.marw.cdt.cmake.core.cmakecache.CMakeCacheFileParser#parse(java.io.InputStream, de.marw.cdt.cmake.core.cmakecache.CMakeCacheFileParser.EntryFilter, Collection, List)}
   * .
   *
   * @throws Exception
   */
  @Test
  public final void testParse() throws Exception {
    String input = "# This is the CMakeCache file.\n"
        + " # For build in directory: /home/me/devel/src/CDT-CMake/CDT-mgmt2-C-1src/build/Debug\n"
        + "\t# It was generated by CMake: /usr/bin/cmake\n"
        + " \t# You can edit this file to change values found and used by cmake.\n"
        + "\t    # If you do not want to change any of the values, simply exit the editor.\n"
        + "  # KEY:TYPE=VALUE\n"
        + "# VALUE is the current value for the KEY.\n" + "\n" + " \t  \n"
        + "########################\n" + "# EXTERNAL cache entries\n"
        + "########################\n" + "\n" + "//Path to a program.\n"
        + "CMAKE_AR:FILEPATH=/usr/bin/ar\n" + "\n"
        + "//Choose the type of build, options are: None(CMAKE_CXX_FLAGS or\n"
        + "// CMAKE_C_FLAGS used) Debug Release RelWithDebInfo MinSizeRel.\n"
        + "CMAKE_BUILD_TYPE:STRING=\n" + "\n"
        + "  //Enable/Disable color output during build.\n"
        + "  CMAKE_COLOR_MAKEFILE:BOOL=ON\n" + "\n" + "//CXX compiler.\n"
        + "CMAKE_CXX_COMPILER:FILEPATH='/usr /b i n/c + +'\n" + "\n"
        + "CMAKE_CXX_FLAGS_DEBUG:STRING='-g '\n" + "\n"
        + "CMAKE_CXX_FLAGS_MINSIZEREL:STRING=-Os -DNDEBUG\n" + "\n"
        + "CMAKE_CXX_FLAGS_RELEASE:STRING=-O3 -DNDEBUG\n" + "\n"
        + "\"CMAKE_CXX_FLAGS_RELWITHDEBINFO\":STRING=-O2 -g -DNDEBUG\n" + "\n"
        + "   \"CMAKE C COMPILER\":FILEPATH=/usr/bin/cc\n" + "\n" + "\n" + "\n"
        + "# INTERNAL cache entries\n" + "########################\n" + "\n"
        + "//ADVANCED property for variable: CMAKE_AR\n"
        + "CMAKE_VERBOSE_MAKEFILE-ADVANCED:INTERNAL=1\n";
    ArrayList<String> errLog = new ArrayList<String>();
    ArrayList<SimpleCMakeCacheEntry> entries = new ArrayList<SimpleCMakeCacheEntry>();
    boolean errors = testee.parse(new ByteArrayInputStream(input.getBytes()),
        null, entries, errLog);
    assertFalse("has errors", errors);
    assertEquals("error msgs", 0, errLog.size());

    input = "CMAKE_AR:FILEPATH=/usr/bin/ar\n" + "CMAKE_BUILD_TYPE:STRING=\n"
        + "  CMAKE_CXX_COMPILER:FILEPATH=/usr/bin/c++\n"
        + " \t  CMAKE_CXX_FLAGS:STRING=\n"
        + "CMAKE_CXX_FLAGS_DEBUG:STRING=-g\n"
        + "\" KEY with Space 1\":STRING=-Os -DNDEBUG\n"
        + "\"\t KEY \twith Space\t 2 \":STRING=-Os -DNDEBUG\n";
    entries = new ArrayList<SimpleCMakeCacheEntry>();
    errLog = new ArrayList<String>();
    errors = testee.parse(new ByteArrayInputStream(input.getBytes()), null,
        entries, errLog);
    assertFalse("has errors", errors);
    assertEquals("error msgs", 0, errLog.size());
    assertEquals("entries", 7, entries.size());
  }

  @Test
  public final void testParse_duplicatedEntries() throws Exception {
    String key = "DUPLICATED_VAR";
    String input = key + ":FILEPATH=/usr/bin/ar\n" + "\n"
        + "CMAKE_BUILD_TYPE:STRING=\n" + "\n" + key + ":BOOL=ON\n" + "\n"
        + "CMAKE_VERBOSE_MAKEFILE-ADVANCED:INTERNAL=1\n";
    // test with LIST
    List<SimpleCMakeCacheEntry> entries = new ArrayList<SimpleCMakeCacheEntry>();
    boolean errors = testee.parse(new ByteArrayInputStream(input.getBytes()),
        null, entries, null);
    assertFalse("has errors", errors);
    assertEquals("duplicates present", 4, entries.size());

    // test with Set
    Set<SimpleCMakeCacheEntry> entrySet = new HashSet<SimpleCMakeCacheEntry>();
    errors = testee.parse(new ByteArrayInputStream(input.getBytes()), null,
        entrySet, null);
    assertFalse("has errors", errors);
    assertEquals("duplicates not present", 3, entrySet.size());
  }

  @Test
  public final void testParse_useFilter() throws Exception {
    final String filteredKey = "FILTERED_VAR";
    String input = filteredKey + ":FILEPATH=/usr/bin/ar\n" + "\n"
        + "CMAKE_BUILD_TYPE:STRING=\n" + "\n"
        + "CMAKE_CXX_FLAGS_DEBUG:STRING=-g\n" + "\n"
        + "CMAKE_VERBOSE_MAKEFILE-ADVANCED:INTERNAL=1\n";

    Set<SimpleCMakeCacheEntry> entrySet = new HashSet<SimpleCMakeCacheEntry>();
    boolean errors = testee.parse(new ByteArrayInputStream(input.getBytes()),
        new EntryFilter() {
          @Override
          public boolean accept(String key) {
            return filteredKey.equals(key);
          }
        }, entrySet, null);
    assertFalse("has errors", errors);
    assertEquals("filtered", 1, entrySet.size());
    assertEquals("filtered present", filteredKey, entrySet.iterator().next()
        .getKey());
  }

  /**
   * Test method for
   * {@link de.marw.cdt.cmake.core.cmakecache.CMakeCacheFileParser#parse(java.io.InputStream, de.marw.cdt.cmake.core.cmakecache.CMakeCacheFileParser.EntryFilter, Collection, List)}
   * .
   *
   * @throws Exception
   */
  @Test
  public final void testParse_ill_Lines() throws Exception {
    String input = "# This is the CMakeCache file.\n"
        + "# For build in directory: /home/me/devel/src/CDT-CMake/CDT-mgmt2-C-1src/build/Debug\n"
        + "# It was generated by CMake: /usr/bin/cmake\n"
        + "# You can edit this file to change values found and used by cmake.\n"
        + "# If you do not want to change any of the values, simply exit the editor.\n"
        + "# If you do want to change a value, simply edit, save, and exit the editor.\n"
        + "# The syntax for the file is as follows:\n"
        + "# KEY:TYPE=VALUE\n"
        + "# KEY is the name of a variable in the cache.\n"
        + "# TYPE is a hint to GUI's for the type of VALUE, DO NOT EDIT TYPE!.\n"
        + "# VALUE is the current value for the KEY.\n"
        + "\n"
        + "########################\n"
        + "# EXTERNAL cache entries\n"
        + "########################\n"
        + "\n"
        + "//Path to a program.\n"
        + "CMAKE_AR:FILEPATH=/usr/bin/ar\n"
        + "\n"
        + "//Choose the type of build, options are: None(CMAKE_CXX_FLAGS or\n"
        + "// CMAKE_C_FLAGS used) Debug Release RelWithDebInfo MinSizeRel.\n"
        + "CMAKE_BUILD_TYPE:STRING=\n"
        + "\n"
        + "//Enable/Disable color output during build.\n"
        + "CMAKE_COLOR_MAKEFILE:BOOL=ON\n"
        + "\n"
        + "//CXX compiler.\n"
        + "CMAKE_CXX_COMPILER:FILEPATH=/usr/bin/c++\n"
        + "\n"
        + "//Flags used by the compiler during all build types.\n"
        + "CMAKE_CXX_FLAGS:STRING=\n"
        + "\n"
        + "//Flags used by the compiler during debug builds.\n"
        + "CMAKE_CXX_FLAGS_DEBUG:STRING=-g\n"
        + "\n"
        + "//Flags used by the compiler during release minsize builds.\n"
        + "CMAKE_CXX_FLAGS_MINSIZEREL:STRING=-Os -DNDEBUG\n"
        + "\n"
        + "//Flags used by the compiler during release builds (/MD /Ob1 /Oi\n"
        + "// /Ot /Oy /Gs will produce slightly less optimized but smaller\n"
        + "// files).\n"
        + "CMAKE_CXX_FLAGS_RELEASE:STRING=-O3 -DNDEBUG\n"
        + "\n"
        + "//Flags used by the compiler during Release with Debug Info builds.\n"
        + "CMAKE_CXX_FLAGS_RELWITHDEBINFO:STRING=-O2 -g -DNDEBUG\n"
        + "\n"
        + "//C compiler.\n"
        + "CMAKE_C_COMPILER:FILEPATH=/usr/bin/cc\n"
        + "\n"
        + "//Flags used by the compiler during all build types.\n"
        + "CMAKE_C_FLAGS:STRING=\n"
        + "\n"
        + "//Flags used by the compiler during debug builds.\n"
        + "CMAKE_C_FLAGS_DEBUG:STRING=-g\n"
        + "\n"
        + "//Flags used by the compiler during release minsize builds.\n"
        + "CMAKE_C_FLAGS_MINSIZEREL:STRING=-Os -DNDEBUG\n"
        + "\n"
        + "//Flags used by the compiler during release builds (/MD /Ob1 /Oi\n"
        + "// /Ot /Oy /Gs will produce slightly less optimized but smaller\n"
        + "// files).\n"
        + "CMAKE_C_FLAGS_RELEASE:STRING=-O3 -DNDEBUG\n"
        + "\n"
        + "//Flags used by the compiler during Release with Debug Info builds.\n"
        + "CMAKE_C_FLAGS_RELWITHDEBINFO:STRING=-O2 -g -DNDEBUG\n"
        + "\n"
        + "//Flags used by the linker.\n"
        + "CMAKE_EXE_LINKER_FLAGS:STRING=' '\n"
        + "\n"
        + "//Flags used by the linker during debug builds.\n"
        + "CMAKE_EXE_LINKER_FLAGS_DEBUG:STRING=\n"
        + "\n"
        + "//Flags used by the linker during release minsize builds.\n"
        + "CMAKE_EXE_LINKER_FLAGS_MINSIZEREL:STRING=\n"
        + "\n"
        + "//Flags used by the linker during release builds.\n"
        + "CMAKE_EXE_LINKER_FLAGS_RELEASE:STRING=\n"
        + "\n"
        + "//Flags used by the linker during Release with Debug Info builds.\n"
        + "CMAKE_EXE_LINKER_FLAGS_RELWITHDEBINFO:STRING=\n"
        + "\n"
        + "//Enable/Disable output of compile commands during generation.\n"
        + "CMAKE_EXPORT_COMPILE_COMMANDS:BOOL=OFF\n"
        + "\n"
        + "//Install path prefix, prepended onto install directories.\n"
        + "CMAKE_INSTALL_PREFIX:PATH=/usr/local\n"
        + "\n"
        + "//Path to a program.\n"
        + "CMAKE_LINKER:FILEPATH=/usr/bin/ld\n"
        + "\n"
        + "//Path to a program.\n"
        + "CMAKE_MAKE_PROGRAM:FILEPATH=/usr/bin/gmake\n"
        + "\n"
        + "//If this value is on, makefiles will be generated without the\n"
        + "// .SILENT directive, and all commands will be echoed to the console\n"
        + "// during the make.  This is useful for debugging only. With Visual\n"
        + "// Studio IDE projects all commands are done without /nologo.\n"
        + "CMAKE_VERBOSE_MAKEFILE:BOOL=FALSE\n" + "\n"
        + "//multiline cache variable\n" + "MYPROJ_VERSION_TXT:STRING=Hello!\n"
        + "  This is my project, {VERSION_MAJOR}.\n"
        + "  {VERSION_MINOR}.{VERSION_PATCH}-\n" + "  {VERSION_EXTRA}.\n"
        + "  Have Fun!\n" + "\n" + "\n" + "########################\n"
        + "# INTERNAL cache entries\n" + "########################\n" + "\n"
        + "//ADVANCED property for variable: CMAKE_AR\n"
        + "CMAKE_AR-ADVANCED:INTERNAL=1\n"
        + "//ADVANCED property for variable: CMAKE_BUILD_TOOL\n"
        + "CMAKE_BUILD_TOOL-ADVANCED:INTERNAL=1\n"
        + "//What is the target build tool cmake is generating for.\n"
        + "CMAKE_BUILD_TOOL:INTERNAL=/usr/bin/gmake\n"
        + "//Install .so files without execute permission.\n"
        + "CMAKE_INSTALL_SO_NO_EXE:INTERNAL=0\n"
        + "//Path to CMake installation.\n"
        + "CMAKE_ROOT:INTERNAL=/usr/share/cmake\n"
        + "//ADVANCED property for variable: CMAKE_SHARED_LINKER_FLAGS\n"
        + "CMAKE_SHARED_LINKER_FLAGS-ADVANCED:INTERNAL=1\n"
        + "//ADVANCED property for variable: CMAKE_SHARED_LINKER_FLAGS_DEBUG\n"
        + "CMAKE_SHARED_LINKER_FLAGS_DEBUG-ADVANCED:INTERNAL=1\n"
        + "//uname command\n" + "CMAKE_UNAME:INTERNAL=/usr/bin/uname\n"
        + "//ADVANCED property for variable: CMAKE_USE_RELATIVE_PATHS\n"
        + "CMAKE_USE_RELATIVE_PATHS-ADVANCED:INTERNAL=1\n"
        + "//ADVANCED property for variable: CMAKE_VERBOSE_MAKEFILE\n"
        + "CMAKE_VERBOSE_MAKEFILE-ADVANCED:INTERNAL=1\n";
    ArrayList<String> errLog = new ArrayList<String>();
    boolean errors = testee.parse(new ByteArrayInputStream(input.getBytes()),
        null, null, errLog);
    assertTrue("has errors", errors);
    assertNotEquals("error msgs", 0, errLog.size());
  }

}
