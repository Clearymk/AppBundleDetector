import os
import re
import argparse

if __name__ == "__main__":
    parser = argparse.ArgumentParser()
    parser.add_argument('-id', help='emulator id')
    args = parser.parse_args()

    emulator_id = args.id

    with os.popen("adb -s " + emulator_id + " shell dumpsys notification --noredact") as fp:
        bf = fp._stream.buffer.read()
    try:
        data = bf.decode().strip()
    except UnicodeDecodeError:
        data = bf.decode("gbk").strip()

    f1 = f2 = False
    for notification in re.findall(r"extras\{.*?}", data, re.S):
        if "Additional file" in notification:
            f1 = True
        if "com.android.vending" in notification:
            f2 = True

    print(f1 and f2)