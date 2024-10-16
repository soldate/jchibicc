package jchibicc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Token {
	
	enum Kind {
		TK_PUNCT, // Punctuators
		TK_NUM, // Numeric literals
		TK_EOF, // End-of-file markers
	}
	
	Kind kind;
	String value;
	int loc; // Token location
	int len; // Token length
	int val; // If kind is TK_NUM, its value

	Token(String value, int start, int end) {
		this.value = value;
		this.loc = start;
		this.len = end - start;
		if (S.isNumeric(value)) {
			kind = Kind.TK_NUM;
			val = Integer.parseInt(value);
		} else kind = Kind.TK_PUNCT;
	}
	
	// just for EOF token
	Token(Kind kind) {
		this.value = "";
		this.kind = kind;
	}	

	boolean equals(String s) {
		return this.value.equals(s);
	}

	public static List<Token> tokenize(String code) {
		String regex = "\\w+|[{}();]|==|<=|>=|!=|\\+\\+|--|&&|\\|\\||[+\\-*/<>=!]";

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(code);

		List<Token> tokens = new ArrayList<>();

		while (matcher.find()) {
			String token = matcher.group();
			int start = matcher.start();
			int end = matcher.end();
			tokens.add(new Token(token, start, end));
		}

		return tokens;
	}
}
