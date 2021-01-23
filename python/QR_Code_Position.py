class QRCodePosition:
    def __init__(self, x, y, width, height, image_width, image_height):
        self._x = x
        self._y = y
        self._width = width
        self._height = height
        self._image_width = image_width
        self._image_height = image_height

    def get_x(self):
        return round(self._x / self._image_width, 6)

    def get_y(self):
        return round(self._y / self._image_height, 6)

    def get_width(self):
        return round(self._width / self._image_width, 6)

    def get_height(self):
        return round(self._height / self._image_height, 6)

    def label_data(self):
        return "0 " + str(self.get_x()) + " " + str(self.get_y()) + " " + str(self.get_width()) + " " + str(
            self.get_height()) + "\n"
