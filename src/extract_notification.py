import os
import re

with os.popen("adb install dumpsys notification --noredact") as fp:
    bf = fp._stream.buffer.read()
try:
    data = bf.decode().strip()
except UnicodeDecodeError:
    data = bf.decode("gbk").strip()

f1 = f2 = False
for notification in re.findall(r"extras\{.*?}", data, re.S)
    if "Additional file" in notification:
        f1 = True
    if "com.android.vending" in notification:
        f2 = True

print(f1 and f2)