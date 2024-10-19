package jchibicc;

import java.io.PrintStream;
import java.util.regex.Pattern;

// S = static methods (boilerplate code)
class S {
	
	private static Pattern pIsNumeric = Pattern.compile("-?\\d+(\\.\\d+)?");
	
	static boolean isNumeric(String strNum) {
		if (strNum == null) {
			return false;
		}
		return pIsNumeric.matcher(strNum).matches();
	}	

	static void error(String s, Object... o) {
		printf(System.err, s, o);
		throw new RuntimeException("ERRO");
		// System.exit(1);
	}

	static void printf(String s, Object... o) {
		printf(System.out, s, o);
	}

	private static void printf(PrintStream out, String s, Object... o) {
		out.printf(s, o);
	}
	
	static boolean isValidCKeyword(String name) {
        // Array of all C keywords
        String[] cKeywords = {
            "auto", "break", "case", "char", "const", "continue", "default", "do", "double", 
            "else", "enum", "extern", "float", "for", "goto", "if", "inline", "int", "long", 
            "register", "restrict", "return", "short", "signed", "sizeof", "static", "struct", 
            "switch", "typedef", "union", "unsigned", "void", "volatile", "while", "_Alignas", 
            "_Alignof", "_Atomic", "_Bool", "_Complex", "_Generic", "_Imaginary", "_Noreturn", 
            "_Static_assert", "_Thread_local"
        };

        // Check if the given name matches any C keyword
        for (String keyword : cKeywords) {
            if (name.equals(keyword)) {
                return true;  // It's a C keyword
            }
        }

        return false;  // Not a C keyword
    }	
	
    static boolean isValidCVariableName(String name) {
        // Regex for valid C variable names: starts with a letter or underscore, followed by letters, numbers, or underscores
        String regex = "^[a-zA-Z_][a-zA-Z0-9_]*$";

        // Check if the string matches the regex
        if (!name.matches(regex)) {
            return false;
        }

        // Check if the given name matches any C keyword
        if (isValidCKeyword(name)) {
        	return false;
        }
        
        return true;  // Passed both checks, it's a valid variable name
    }	
    
    static boolean isValidCPunctuator(String str) {
        // List of all valid C punctuators
        String[] punctuators = {
            "[", "]", "(", ")", "{", "}", ".", "->", "++", "--", "&", "*", "+", "-", "~", "!",
            "/", "%", "<<", ">>", "<", ">", "<=", ">=", "==", "!=", "^", "|", "&&", "||", "?",
            ":", ";", "...", "=", "*=", "/=", "%=", "+=", "-=", "<<=", ">>=", "&=", "^=", "|=",
            ",", "#", "##", "<:", ":>", "<%", "%>", "%:", "%:%:"
        };

        // Check if the string is in the list of valid punctuators
        for (String punctuator : punctuators) {
            if (str.equals(punctuator)) {
                return true;  // It's a valid punctuator
            }
        }

        return false;  // Not a valid punctuator
    }    
	
}
