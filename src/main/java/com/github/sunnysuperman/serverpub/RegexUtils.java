package com.github.sunnysuperman.serverpub;

import java.util.Map;

public class RegexUtils {

	public static String replaceRegexKeywords(String s) {
		StringBuilder buf = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			if (c == '\\') {
				buf.append("\\\\");
			} else if (c == '$') {
				buf.append("\\$");
			} else {
				buf.append(c);
			}
		}
		return buf.toString();
	}

	public static interface CompileHandler {
		String compile(String key, Map<String, String> context);
	}

	private static final CompileHandler DEFAULT_COMPILE_HANDLER = new CompileHandler() {

		@Override
		public String compile(String key, Map<String, String> context) {
			Object value = context.get(key);
			if (value == null) {
				return null;
			}
			return value.toString();
		}

	};

	public static final String compile(final String s, Map<String, String> context, boolean retainKey,
			CompileHandler handler) {
		if (s == null) {
			return null;
		}
		if (handler == null) {
			handler = DEFAULT_COMPILE_HANDLER;
		}
		StringBuffer buf = new StringBuffer();
		int fromIndex = 0;
		int dollarIndex = 0;
		int len = s.length();
		while ((dollarIndex = s.indexOf('$', fromIndex)) >= 0) {
			int bracketStartIndex = dollarIndex + 1;
			if (bracketStartIndex < len && s.charAt(bracketStartIndex) == '{') {
				int bracketEndIndex = s.indexOf('}', bracketStartIndex + 1);
				if (bracketEndIndex < 0) {
					throw new RuntimeException("No bracket end");
				}
				String key = s.substring(bracketStartIndex + 1, bracketEndIndex);
				if (key.isEmpty()) {
					throw new RuntimeException("Empty key");
				}
				String value = handler.compile(key, context);
				if (dollarIndex > fromIndex) {
					buf.append(s.substring(fromIndex, dollarIndex));
				}
				if (value == null) {
					if (retainKey) {
						buf.append("${").append(key).append('}');
					}
				} else {
					buf.append(value);
				}
				fromIndex = bracketEndIndex + 1;
			} else {
				if (dollarIndex > fromIndex) {
					buf.append(s.substring(fromIndex, dollarIndex));
				}
				buf.append('$');
				fromIndex = dollarIndex + 1;
			}
		}
		if (fromIndex < len) {
			if (fromIndex == 0) {
				return s;
			}
			buf.append(s.substring(fromIndex));
		}
		return buf.toString();
	}

	public static final String compile(final String s, Map<String, String> context, boolean retainKey) {
		return compile(s, context, retainKey, null);
	}

	public static final String compile(final String content, Map<String, String> context) {
		return compile(content, context, false, null);
	}

}
