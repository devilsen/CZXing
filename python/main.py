# This is a sample Python script.

# Press Shift+F10 to execute it or replace it with your code.
# Press Double Shift to search everywhere for classes, files, tool windows, actions, and settings.
from Labeler import Labeler
from QR_Extractor import QR_Extractor


def print_hi(name):
    # Use a breakpoint in the code line below to debug your script.
    print(f'Hi, {name}')  # Press Ctrl+F8 to toggle the breakpoint.


# zbar https://github.com/primetang/qrtools
def decode_qr_ode():
    qr_scanner = QR_Extractor()
    output = qr_scanner.extract('image/multi_qr_code.png')
    print(output)
    # output = qr_scanner.extract('image/test3.png')
    # print(output)


def test_label():
    labeler = Labeler()
    # labeler.read_dir("C:\\Users\\37591\\Desktop\\testimage\\eeee")
    labeler.read_dir("C:\\Users\\37591\\Downloads\\wechatqrcode\\test555")
    # labeler.read_dir("image")


# Press the green button in the gutter to run the script.
if __name__ == '__main__':
    # decode_qr_ode()
    test_label()

# See PyCharm help at https://www.jetbrains.com/help/pycharm/
