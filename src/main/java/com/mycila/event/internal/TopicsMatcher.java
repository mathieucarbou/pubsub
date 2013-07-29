/**
 * Copyright (C) 2010 Mycila (mathieu.carbou@gmail.com)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.mycila.event.internal;

import com.mycila.event.Topic;
import com.mycila.event.Topics;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.StringTokenizer;

import static com.mycila.event.internal.Ensure.notNull;

public final class TopicsMatcher extends Topics implements Serializable {

    private static final long serialVersionUID = 0;
    private static final String DEFAULT_PATH_SEPARATOR = "/";
    private final String pattern;

    private TopicsMatcher(String pattern) {
        this.pattern = notNull(pattern, "Pattern");
    }

    public boolean matches(Topic target) {
        return doMatch(pattern, notNull(target, "Topic").getName(), true);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof TopicsMatcher
                && ((TopicsMatcher) o).pattern.equals(pattern);
    }

    @Override
    public int hashCode() {
        return 31 * pattern.hashCode();
    }

    @Override
    public String toString() {
        return pattern;
    }

    public static Topics forPattern(String pattern) {
        return new TopicsMatcher(pattern);
    }

    private static boolean doMatch(String pattern, String path, boolean fullMatch) {
        if (path.startsWith(DEFAULT_PATH_SEPARATOR) != pattern.startsWith(DEFAULT_PATH_SEPARATOR))
            return false;
        String[] pattDirs = tokenizeToStringArray(pattern, DEFAULT_PATH_SEPARATOR);
        String[] pathDirs = tokenizeToStringArray(path, DEFAULT_PATH_SEPARATOR);
        int pattIdxStart = 0;
        int pattIdxEnd = pattDirs.length - 1;
        int pathIdxStart = 0;
        int pathIdxEnd = pathDirs.length - 1;
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            String patDir = pattDirs[pattIdxStart];
            if ("**".equals(patDir))
                break;
            if (!matchStrings(patDir, pathDirs[pathIdxStart]))
                return false;
            pattIdxStart++;
            pathIdxStart++;
        }
        if (pathIdxStart > pathIdxEnd) {
            if (pattIdxStart > pattIdxEnd)
                return pattern.endsWith(DEFAULT_PATH_SEPARATOR) ? path.endsWith(DEFAULT_PATH_SEPARATOR) : !path.endsWith(DEFAULT_PATH_SEPARATOR);
            if (!fullMatch)
                return true;
            if (pattIdxStart == pattIdxEnd && pattDirs[pattIdxStart].equals("*") && path.endsWith(DEFAULT_PATH_SEPARATOR))
                return true;
            for (int i = pattIdxStart; i <= pattIdxEnd; i++)
                if (!pattDirs[i].equals("**"))
                    return false;
            return true;
        } else if (pattIdxStart > pattIdxEnd)
            return false;
        else if (!fullMatch && "**".equals(pattDirs[pattIdxStart]))
            return true;
        while (pattIdxStart <= pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            String patDir = pattDirs[pattIdxEnd];
            if (patDir.equals("**"))
                break;
            if (!matchStrings(patDir, pathDirs[pathIdxEnd]))
                return false;
            pattIdxEnd--;
            pathIdxEnd--;
        }
        if (pathIdxStart > pathIdxEnd) {
            for (int i = pattIdxStart; i <= pattIdxEnd; i++)
                if (!pattDirs[i].equals("**"))
                    return false;
            return true;
        }
        while (pattIdxStart != pattIdxEnd && pathIdxStart <= pathIdxEnd) {
            int patIdxTmp = -1;
            for (int i = pattIdxStart + 1; i <= pattIdxEnd; i++) {
                if (pattDirs[i].equals("**")) {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == pattIdxStart + 1) {
                pattIdxStart++;
                continue;
            }
            int patLength = (patIdxTmp - pattIdxStart - 1);
            int strLength = (pathIdxEnd - pathIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    String subPat = pattDirs[pattIdxStart + j + 1];
                    String subStr = pathDirs[pathIdxStart + i + j];
                    if (!matchStrings(subPat, subStr))
                        continue strLoop;
                }
                foundIdx = pathIdxStart + i;
                break;
            }
            if (foundIdx == -1)
                return false;
            pattIdxStart = patIdxTmp;
            pathIdxStart = foundIdx + patLength;
        }
        for (int i = pattIdxStart; i <= pattIdxEnd; i++)
            if (!pattDirs[i].equals("**"))
                return false;
        return true;
    }

    private static String[] toStringArray(Collection<String> collection) {
        if (collection == null)
            return null;
        return collection.toArray(new String[collection.size()]);
    }

    private static String[] tokenizeToStringArray(String str, String delimiters) {
        return tokenizeToStringArray(str, delimiters, true, true);
    }

    private static String[] tokenizeToStringArray(String str, String delimiters, boolean trimTokens, boolean ignoreEmptyTokens) {
        if (str == null)
            return null;
        StringTokenizer st = new StringTokenizer(str, delimiters);
        List<String> tokens = new ArrayList<String>();
        while (st.hasMoreTokens()) {
            String token = st.nextToken();
            if (trimTokens)
                token = token.trim();
            if (!ignoreEmptyTokens || token.length() > 0)
                tokens.add(token);
        }
        return toStringArray(tokens);
    }

    private static boolean matchStrings(String pattern, String str) {
        char[] patArr = pattern.toCharArray();
        char[] strArr = str.toCharArray();
        int patIdxStart = 0;
        int patIdxEnd = patArr.length - 1;
        int strIdxStart = 0;
        int strIdxEnd = strArr.length - 1;
        char ch;
        boolean containsStar = false;
        for (char aPatArr : patArr) {
            if (aPatArr == '*') {
                containsStar = true;
                break;
            }
        }
        if (!containsStar) {
            if (patIdxEnd != strIdxEnd)
                return false;
            for (int i = 0; i <= patIdxEnd; i++) {
                ch = patArr[i];
                if (ch != '?' && ch != strArr[i])
                    return false;
            }
            return true;
        }
        if (patIdxEnd == 0)
            return true;
        while ((ch = patArr[patIdxStart]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?' && ch != strArr[strIdxStart])
                return false;
            patIdxStart++;
            strIdxStart++;
        }
        if (strIdxStart > strIdxEnd) {
            for (int i = patIdxStart; i <= patIdxEnd; i++)
                if (patArr[i] != '*')
                    return false;
            return true;
        }
        while ((ch = patArr[patIdxEnd]) != '*' && strIdxStart <= strIdxEnd) {
            if (ch != '?' && ch != strArr[strIdxEnd])
                return false;
            patIdxEnd--;
            strIdxEnd--;
        }
        if (strIdxStart > strIdxEnd) {
            for (int i = patIdxStart; i <= patIdxEnd; i++)
                if (patArr[i] != '*')
                    return false;
            return true;
        }
        while (patIdxStart != patIdxEnd && strIdxStart <= strIdxEnd) {
            int patIdxTmp = -1;
            for (int i = patIdxStart + 1; i <= patIdxEnd; i++) {
                if (patArr[i] == '*') {
                    patIdxTmp = i;
                    break;
                }
            }
            if (patIdxTmp == patIdxStart + 1) {
                patIdxStart++;
                continue;
            }
            int patLength = (patIdxTmp - patIdxStart - 1);
            int strLength = (strIdxEnd - strIdxStart + 1);
            int foundIdx = -1;
            strLoop:
            for (int i = 0; i <= strLength - patLength; i++) {
                for (int j = 0; j < patLength; j++) {
                    ch = patArr[patIdxStart + j + 1];
                    if (ch != '?' && ch != strArr[strIdxStart + i + j])
                        continue strLoop;
                }
                foundIdx = strIdxStart + i;
                break;
            }
            if (foundIdx == -1)
                return false;
            patIdxStart = patIdxTmp;
            strIdxStart = foundIdx + patLength;
        }

        for (int i = patIdxStart; i <= patIdxEnd; i++)
            if (patArr[i] != '*')
                return false;
        return true;
    }

}
