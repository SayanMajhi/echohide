# College demonstration guide

Status: this is the script for the final application, not a claim of an executable Phase 1 demonstration. Phase 8 will add generated assets and exact commands for the completed GUI. Today only the foundation commands in the README work.

## Planned seven-minute presentation

| Time | Topic and action |
|---|---|
| 0:00–0:40 | Explain motivation: conceal a short encrypted message inside an ordinary playable WAV. Define encryption (protect content) and steganography (hide placement). |
| 0:40–1:20 | Show architecture and flow. Identify 16-bit PCM samples, one LSB/sample, mono/stereo capacity and why lossless WAV is required. |
| 1:20–2:00 | Explain password-derived AES-256-GCM, public random salt/nonce, authentication and versioned header. Password is shared separately. |
| 2:00–3:15 | Sender selects public demo WAV, plays it, enters public sample text and a temporary password, reviews capacity, embeds and saves a new file. Play modified audio. |
| 3:15–4:20 | Receiver starts on localhost; sender sends the saved WAV. Show progress, receiver-confirmed success and SHA-256 match. Explain TCP framing and ACK. |
| 4:20–5:10 | Receiver extracts with correct password and compares recovered text. Try wrong password; show authentication failure with no plaintext. |
| 5:10–6:10 | Show measured payload/capacity, SNR, embedding/extraction and transfer times. Identify the actual run/environment and recovery result. |
| 6:10–7:00 | Explain limits: detectable LSBs, MP3/resampling damage, no peer authentication in plain TCP, no live call. Brief future TLS/adaptive selection ideas. |

## Final rehearsal checklist

Build from a clean checkout; run all tests; generate original/stego samples with only public demonstration content; open two independent application processes; verify microphone-independent import; keep a known correct password outside committed files; rehearse wrong-password and invalid-format errors. Record actual commands once the final CLI/GUI entry is implemented. A LAN demonstration is optional when a second machine and firewall access are available; localhost remains required.

Capture sender, receiver, metrics and authentication-error screenshots from the actual GUI after implementation. Do not create mock screenshots to stand in for runtime evidence. Record whether the listening impression agrees with the measured SNR. Confirm that closing a receiver and restarting releases the port. Keep backup generated demo assets for presentation-day device/network problems.
