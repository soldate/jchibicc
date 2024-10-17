package jchibicc;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Token {
	
	enum Kind {
		PUNCT, // Punctuators
		NUM,   // Numeric literals
		EOF,   // End-of-file markers
	}
	
	Kind kind;
	Token next; // Next token
	String str; // token string
	int loc;    // Token location
	int len;    // Token length
	int val;    // If kind is TK_NUM, its value

	Token(String value, int start, int end) {
		this.str = value;
		this.loc = start;
		this.len = end - start;
		if (S.isNumeric(value)) {
			kind = Kind.NUM;
			val = Integer.parseInt(value);
		} else kind = Kind.PUNCT;
	}
	
	// just for EOF token
	Token(Kind kind) {
		this.str = "";
		this.kind = kind;
	}	

	boolean equals(String s) {
		return this.str.equals(s);
	}

	@Override
	public String toString() {
		return str;
	}

	public static Token tokenize(String code) {
		String regex = "\\w+|[{}();]|==|<=|>=|!=|\\+\\+|--|&&|\\|\\||[+\\-*/<>=!]";

		Pattern pattern = Pattern.compile(regex);
		Matcher matcher = pattern.matcher(code);

		List<Token> tokens = new ArrayList<>();

		Token last = null;
		while (matcher.find()) {			
			String token = matcher.group();
			int start = matcher.start();
			int end = matcher.end();
			
			// each token points to the next
			Token newToken = new Token(token, start, end);
			if (last != null) last.next = newToken;
			tokens.add(newToken);
			last = newToken;
		}
		
		// eof token
		Token newToken = new Token(Token.Kind.EOF);
		if (last != null) last.next = newToken;		
		tokens.add(newToken);
		
		return tokens.get(0);
	}
}
