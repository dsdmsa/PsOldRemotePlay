#!/usr/bin/env python3
"""
extract_aes_patterns.py — Scan source files for AES/crypto patterns

Finds AES/CBC/CFB/ECB references, key/IV variable names and values,
XOR operations near crypto code, and base64 encode/decode near crypto.

Usage: ./extract_aes_patterns.py [repos_dir]
"""

import os
import re
import sys
import json
from pathlib import Path
from dataclasses import dataclass, field, asdict
from typing import Optional

REPOS_DIR = "/Users/mihailurmanschi/Work/PsOldRemotePlay/research/repos"
SOURCE_EXTENSIONS = {'.c', '.cpp', '.h', '.hpp', '.py', '.java', '.kt', '.kts',
                     '.cs', '.js', '.ts', '.go', '.rs', '.swift', '.m', '.mm'}

# Patterns to search for
CRYPTO_PATTERNS = {
    'aes_mode': re.compile(
        r'\b(AES[_\-]?(128|192|256)?[_\-]?(CBC|CFB|ECB|CTR|GCM|OFB)|'
        r'AES\.new|AES\.encrypt|AES\.decrypt|Cipher\.getInstance.*AES|'
        r'EVP_aes_|aes_cbc_|aes_ecb_|AES_set_[de]crypt_key|'
        r'CCCrypt|kCCAlgorithmAES|RIJNDAEL|rijndael|'
        r'CryptoJS\.AES|crypto\.createCipher|Aes\.|AesManaged|'
        r'AES_KEY|aes_key_t|mbedtls_aes)',
        re.IGNORECASE
    ),
    'key_iv_def': re.compile(
        r'\b((?:aes[_]?)?(?:key|iv|nonce|salt|secret|cipher[_]?key|'
        r'enc[_]?key|dec[_]?key|session[_]?key|master[_]?key|'
        r'iv[_]?base|iv[_]?context|skey|pkey|km[_]?data|'
        r'reg[_]?key|reg[_]?iv|xor[_]?key))\s*[=:]',
        re.IGNORECASE
    ),
    'hex_array': re.compile(
        r'(?:0x[0-9a-fA-F]{2}[\s,]+){4,}0x[0-9a-fA-F]{2}|'  # 0xNN, 0xNN, ...
        r'(?:\\x[0-9a-fA-F]{2}){4,}|'  # \xNN\xNN...
        r'bytes?\s*\(\s*\[\s*(?:0x[0-9a-fA-F]{2}[\s,]*){4,}|'  # bytes([0xNN, ...
        r'byteArrayOf\s*\(\s*(?:0x[0-9a-fA-F]{2})',  # byteArrayOf(0xNN
        re.IGNORECASE
    ),
    'xor_operation': re.compile(
        r'\^=?\s*(?:0x[0-9a-fA-F]+|key|iv|nonce|xor|mask)|'
        r'(?:key|iv|nonce|xor|mask)\s*\^|'
        r'XOR|xor_bytes|byte_xor',
        re.IGNORECASE
    ),
    'base64_crypto': re.compile(
        r'\b(?:base64[_\.]?(?:encode|decode|b64encode|b64decode|urlsafe)|'
        r'btoa|atob|Base64\.encode|Base64\.decode|'
        r'toBase64|fromBase64|encodeBase64|decodeBase64|'
        r'android\.util\.Base64)',
        re.IGNORECASE
    ),
    'key_derivation': re.compile(
        r'\b(?:PBKDF2|HKDF|scrypt|bcrypt|derive[_]?key|'
        r'key[_]?derivat|KDF|hmac[_]?sha|SHA256|SHA1|MD5|'
        r'EVP_BytesToKey|PKCS5|PKCS7)',
        re.IGNORECASE
    ),
}

CONTEXT_LINES = 5


@dataclass
class Match:
    file: str
    line_num: int
    pattern_type: str
    matched_text: str
    context_before: list = field(default_factory=list)
    context_after: list = field(default_factory=list)


def scan_file(filepath: Path) -> list:
    """Scan a single source file for crypto patterns."""
    matches = []
    try:
        with open(filepath, 'r', encoding='utf-8', errors='replace') as f:
            lines = f.readlines()
    except (OSError, PermissionError):
        return matches

    for line_idx, line in enumerate(lines):
        for pattern_name, pattern in CRYPTO_PATTERNS.items():
            for m in pattern.finditer(line):
                ctx_before = [
                    lines[i].rstrip()
                    for i in range(max(0, line_idx - CONTEXT_LINES), line_idx)
                ]
                ctx_after = [
                    lines[i].rstrip()
                    for i in range(line_idx + 1, min(len(lines), line_idx + CONTEXT_LINES + 1))
                ]
                matches.append(Match(
                    file=str(filepath),
                    line_num=line_idx + 1,
                    pattern_type=pattern_name,
                    matched_text=m.group(0).strip(),
                    context_before=ctx_before,
                    context_after=ctx_after,
                ))
                break  # One match per pattern per line

    return matches


def print_match(match: Match, repos_dir: str):
    """Pretty-print a single match."""
    rel_path = os.path.relpath(match.file, repos_dir)
    print(f"\n  \033[1;33m[{match.pattern_type}]\033[0m {rel_path}:{match.line_num}")
    print(f"  Matched: \033[1;32m{match.matched_text}\033[0m")
    if match.context_before:
        for cl in match.context_before:
            print(f"    \033[2m{cl}\033[0m")
    # Highlight the matched line
    print(f"    \033[1m>>> {match.context_before and '' or ''}", end='')
    # Find the actual line
    try:
        with open(match.file, 'r', errors='replace') as f:
            for i, line in enumerate(f):
                if i == match.line_num - 1:
                    print(f"{line.rstrip()}\033[0m")
                    break
    except (OSError, PermissionError):
        print(f"(could not re-read line)\033[0m")
    if match.context_after:
        for cl in match.context_after:
            print(f"    \033[2m{cl}\033[0m")


def main():
    repos_dir = sys.argv[1] if len(sys.argv) > 1 else REPOS_DIR

    if repos_dir in ('-h', '--help'):
        print(__doc__.strip())
        sys.exit(0)

    if not os.path.isdir(repos_dir):
        print(f"ERROR: Directory not found: {repos_dir}", file=sys.stderr)
        sys.exit(1)

    print(f"Scanning {repos_dir} for AES/crypto patterns...")
    print(f"Extensions: {', '.join(sorted(SOURCE_EXTENSIONS))}")
    print()

    all_matches = []
    files_scanned = 0

    for root, dirs, files in os.walk(repos_dir):
        # Skip hidden dirs and common non-code dirs
        dirs[:] = [d for d in dirs if not d.startswith('.') and d not in
                   ('node_modules', '__pycache__', '.git', 'build', 'target')]
        for fname in files:
            ext = os.path.splitext(fname)[1].lower()
            if ext not in SOURCE_EXTENSIONS:
                continue
            filepath = Path(root) / fname
            files_scanned += 1
            matches = scan_file(filepath)
            all_matches.extend(matches)

    # Group by repo
    repo_matches = {}
    for m in all_matches:
        rel = os.path.relpath(m.file, repos_dir)
        repo = rel.split(os.sep)[0]
        repo_matches.setdefault(repo, []).append(m)

    # Print results grouped by repo
    for repo in sorted(repo_matches.keys()):
        matches = repo_matches[repo]
        print(f"\033[1;36m{'='*60}\033[0m")
        print(f"\033[1;36mRepo: {repo} ({len(matches)} matches)\033[0m")
        print(f"\033[1;36m{'='*60}\033[0m")

        # Sub-group by pattern type
        by_type = {}
        for m in matches:
            by_type.setdefault(m.pattern_type, []).append(m)

        for ptype in sorted(by_type.keys()):
            print(f"\n\033[1;35m  --- {ptype} ({len(by_type[ptype])} matches) ---\033[0m")
            for m in by_type[ptype][:50]:  # Cap at 50 per type per repo
                print_match(m, repos_dir)
            if len(by_type[ptype]) > 50:
                print(f"  ... and {len(by_type[ptype]) - 50} more")

    # Summary
    print(f"\n\033[1;33m{'='*60}\033[0m")
    print(f"\033[1;33mSummary\033[0m")
    print(f"\033[1;33m{'='*60}\033[0m")
    print(f"Files scanned: {files_scanned}")
    print(f"Total matches: {len(all_matches)}")
    print(f"Repos with matches: {len(repo_matches)}")

    by_type_total = {}
    for m in all_matches:
        by_type_total[m.pattern_type] = by_type_total.get(m.pattern_type, 0) + 1
    for ptype, count in sorted(by_type_total.items(), key=lambda x: -x[1]):
        print(f"  {ptype}: {count}")

    # Optionally dump JSON
    json_path = os.path.join(os.path.dirname(os.path.abspath(__file__)), "aes_patterns_report.json")
    with open(json_path, 'w') as f:
        json.dump([asdict(m) for m in all_matches], f, indent=2)
    print(f"\nFull report written to: {json_path}")


if __name__ == '__main__':
    main()
