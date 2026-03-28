---
name: no_ascii_tables
description: Don't use ASCII/Unicode box-drawing tables — they break when pasted into Reddit/Discord/etc
type: feedback
---

Don't format tables with box-drawing characters (┌─┬│├┤└┘). They look broken when pasted into Reddit comments or other platforms. Use markdown tables or plain bullet lists instead.

**Why:** User needs to copy-paste output directly into Reddit comments where ASCII art tables render poorly.

**How to apply:** For any content the user might paste externally, use simple markdown tables (`| col | col |`) or bullet/list formatting.
