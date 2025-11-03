import sys
import wave
import struct
import os
import sys

input_file = sys.argv[1]
output_file = sys.argv[2]

if not os.path.exists(input_file):
    print(f"Input file not found: {input_file}")
    sys.exit(1)

with wave.open(input_file, 'rb') as wf:
    params = wf.getparams()
    n_channels, sampwidth, framerate, n_frames = params[:4]
    frames = wf.readframes(n_frames)

if sampwidth != 2:
    print(f"Unsupported sample width: {sampwidth}")
    sys.exit(1)

samples = struct.unpack('<' + 'h' * n_frames * n_channels, frames)

factor = 0.5
adjusted = [max(min(int(s * factor), 32767), -32768) for s in samples]

frames_out = struct.pack('<' + 'h' * len(adjusted), *adjusted)

with wave.open(output_file, 'wb') as wf:
    wf.setparams(params)
    wf.writeframes(frames_out)

print(f"Processed {len(adjusted)} samples, saved to {output_file}")
