/*
 * Copyright (C) 2007 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dx.command.dump;

import com.android.dx.cf.iface.ParseException;
import com.android.dx.command.DxConsole;
import com.android.dx.util.FileUtils;
import com.android.dx.util.HexParser;

import java.io.UnsupportedEncodingException;

/**
 * Main class for the class file dumper.
 */
public class Main {

    static Args parsedArgs = new Args();

    /**
     * This class is uninstantiable.
     */
    private Main() {
        // This space intentionally left blank.
    }

    /**
     * Run!
     */
    public static short main(String[] args) {
        int at = 0;

        for (/*at*/; at < args.length; at++) {
            String arg = args[at];
            if (arg.equals("--") || !arg.startsWith("--")) {
                break;
            } else if (arg.equals("--bytes")) {
                parsedArgs.rawBytes = true;
            } else if (arg.equals("--basic-blocks")) {
                parsedArgs.basicBlocks = true;
            } else if (arg.equals("--rop-blocks")) {
                parsedArgs.ropBlocks = true;
            } else if (arg.equals("--optimize")) {
                parsedArgs.optimize = true;
            } else if (arg.equals("--ssa-blocks")) {
                parsedArgs.ssaBlocks = true;
            } else if (arg.startsWith("--ssa-step=")) {
                parsedArgs.ssaStep = arg.substring(arg.indexOf('=') + 1);
            } else if (arg.equals("--debug")) {
                parsedArgs.debug = true;
            } else if (arg.equals("--dot")) {
                parsedArgs.dotDump = true;
            } else if (arg.equals("--strict")) {
                parsedArgs.strictParse = true;
            } else if (arg.startsWith("--width=")) {
                arg = arg.substring(arg.indexOf('=') + 1);
                parsedArgs.width = Integer.parseInt(arg);
            } else if (arg.startsWith("--method=")) {
                arg = arg.substring(arg.indexOf('=') + 1);
                parsedArgs.method = arg;
            } else {
                DxConsole.err.println("unknown option: " + arg);
                DxConsole.err.println("usage");
				return 2;
            }
        }

        if (at == args.length) {
            DxConsole.err.println("no input files specified");
            DxConsole.err.println("usage");
			return 2;
        }

        for (/*at*/; at < args.length; at++) {
            try {
                String name = args[at];
                DxConsole.out.println("reading " + name + "...");
                byte[] bytes = FileUtils.readFile(name);
                if (!name.endsWith(".class")) {
                    String src;
                    try {
                        src = new String(bytes, "utf-8");
                    } catch (UnsupportedEncodingException ex) {
						DxConsole.err.println("shouldn't happen");
                        ex.printStackTrace(DxConsole.err);
						return 2;
                    }
                    bytes = HexParser.parse(src);
                }
                processOne(name, bytes);
            } catch (ParseException ex) {
                DxConsole.err.println("\ntrouble parsing:");
                if (parsedArgs.debug) {
                    ex.printStackTrace(DxConsole.err);
                } else {
                    ex.printContext(DxConsole.err);
                }
				return 2;
            }
        }
		
		return 0;
    }

    /**
     * Processes one file.
     *
     * @param name {@code non-null;} name of the file
     * @param bytes {@code non-null;} contents of the file
     */
    private static void processOne(String name, byte[] bytes) {
        if (parsedArgs.dotDump) {
            DotDumper.dump(bytes, name, parsedArgs);
        } else if (parsedArgs.basicBlocks) {
            BlockDumper.dump(bytes, DxConsole.out, name, false, parsedArgs);
        } else if (parsedArgs.ropBlocks) {
            BlockDumper.dump(bytes, DxConsole.out, name, true, parsedArgs);
        } else if (parsedArgs.ssaBlocks) {
            // --optimize ignored with --ssa-blocks
            parsedArgs.optimize = false;
            SsaDumper.dump(bytes, DxConsole.out, name, parsedArgs);
        } else {
            ClassDumper.dump(bytes, DxConsole.out, name, parsedArgs);
        }
    }
}
