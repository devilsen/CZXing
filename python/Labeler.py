import os
import shutil

from PIL import Image as ImageReader
from QR_Code_Position import QRCodePosition
from QR_Extractor import QR_Extractor


def is_image(image_file):
    split_file = image_file.split(".")
    if len(split_file) <= 1:
        return False
    else:
        file_suffix = split_file[1]
        return file_suffix == "jpg" or file_suffix == "png" or file_suffix == "jpeg"


def get_image_size(image_path):
    return ImageReader.open(image_path).size


class Labeler:
    abs_dir_path = ""
    abs_parent_dir_path = ""
    abs_result_dir_path = ""
    result_dir_name = "result"
    abs_fail_dir_path = ""
    fail_dir_name = "decodeFail"

    index = 1
    image_size_per_dir = 1000

    total = 0
    fail = 0
    success = 0

    def __init__(self):
        self.qr_scanner = QR_Extractor()
        self.abs_parent_dir_path = os.path.abspath(".")
        self.abs_result_dir_path = os.path.join(self.abs_parent_dir_path, self.result_dir_name)
        self.abs_fail_dir_path = os.path.join(self.abs_parent_dir_path, self.fail_dir_name)

        print(self.abs_result_dir_path)
        self.init_index()

    def read_dir(self, dir_path):
        print("select path = " + dir_path)
        # read all images
        images = os.listdir(dir_path)
        self.start_label(os.path.abspath(dir_path), images)
        print("识别完成 Total = " + str(self.total) + " 成功识别 = " + str(self.success) + " 失败 = " + str(self.fail))

    def start_label(self, dir_path, images):
        self.abs_dir_path = os.path.abspath(dir_path)
        self.total += len(images)
        for image_file in images:
            # print("start label image = " + image_file)
            if not is_image(image_file):
                continue

            image_path = os.path.join(dir_path, image_file)
            image_name = image_file.split(".")[0]
            image_suffix = image_file.split(".")[1]
            print("image path = " + image_path)

            image_size = get_image_size(image_path)
            print("image size = " + str(image_size))

            qr_code_results = self.decode_qr_code(image_path)
            if qr_code_results is None or len(qr_code_results) <= 0:
                print("no qr code")
                self.collect_special_image(image_path)
                self.fail += 1
                continue

            # 转化为 Yolo 位置
            label_data_list = self.translate_data(image_size, qr_code_results)

            # 保存同名文件
            label_string = self.get_result_data(label_data_list)
            self.save_file(image_suffix, image_path, label_string)

            print("----------------------- one image done ---------------------------\n")

    def init_index(self):
        result_dir = self.abs_result_dir_path
        if not os.path.exists(result_dir):
            os.mkdir(result_dir)
        self.index = len(os.listdir(result_dir)) * self.image_size_per_dir

        fail_dir = self.abs_fail_dir_path
        if not os.path.exists(fail_dir):
            os.mkdir(fail_dir)

    def translate_data(self, image_size, qr_code_results):
        label_data = []
        for qr_code_result in qr_code_results:
            label_data.append(
                self.translate_to_yolo_coordinate(image_size, qr_code_result["points"])
            )
        return label_data

    def translate_to_yolo_coordinate(self, image_size, qr_code_points):
        image_width, image_height = image_size
        left_top = qr_code_points[0]
        right_top = qr_code_points[1]
        right_bottom = qr_code_points[2]
        left_bottom = qr_code_points[3]

        center_top_x = (left_top[0] + right_top[0]) / 2
        center_bottom_x = (left_bottom[0] + right_bottom[0]) / 2
        center_x = (center_top_x + center_bottom_x) / 2

        center_top_y = (left_top[1] + left_bottom[1]) / 2
        center_bottom_y = (right_top[1] + right_bottom[1]) / 2
        center_y = (center_top_y + center_bottom_y) / 2

        qr_code_width = right_top[0] - left_top[0]
        qr_code_height = left_bottom[1] - left_top[1]

        return QRCodePosition(center_x, center_y, qr_code_width, qr_code_height, image_width, image_height)

    # zbar https://github.com/primetang/qrtools
    def decode_qr_code(self, image_path):
        try:
            qr_code_data = self.qr_scanner.extract(image_path)
            print(qr_code_data)
            return qr_code_data
        except:
            print("Can't open the file")
            return None

    def get_result_data(self, label_data_list):
        label_string = ""
        for label in label_data_list:
            print(label.label_data())
            label_string += label.label_data()

        return label_string

    def save_file(self, image_suffix, image_path, label_string):
        dir_index: int = int(self.index / self.image_size_per_dir)
        target_dir = os.path.join(self.abs_result_dir_path, str(dir_index))
        print("target path = " + target_dir)
        if not os.path.exists(target_dir):
            os.mkdir(target_dir)

        index_string = str(self.index)
        target_name = "{0}\\{1}.{2}".format(target_dir, index_string, image_suffix)
        if os.path.exists(target_name):
            print("The file have been created, please check " + target_name)
            return

        # 将图片复制过去，每1000张一个文件夹
        os.rename(image_path, target_name)

        f = open("{0}\\{1}.txt".format(target_dir, index_string), 'w')
        f.write(str(label_string))
        f.close()

        self.index += 1
        self.success += 1

    def collect_special_image(self, image_path):
        print("recognize fail, move fail to special dir, the file = " + image_path + "\n")

        dir_index: int = int(self.index / self.image_size_per_dir)
        target_dir = os.path.join(self.abs_fail_dir_path, str(dir_index))

        if not os.path.exists(target_dir):
            os.mkdir(target_dir)
        shutil.copy(image_path, target_dir)
