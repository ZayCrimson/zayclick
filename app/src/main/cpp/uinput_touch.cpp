#include <linux/input.h>
#include <linux/uinput.h>
#include <sys/ioctl.h>
#include <sys/time.h>
#include <fcntl.h>
#include <unistd.h>
#include <cerrno>
#include <cstring>
#include <iostream>
#include <sstream>
#include <string>

namespace {
constexpr int kSlots = 10;
int g_fd = -1;
int g_nextTrackingId = 1;
bool g_active[kSlots] = {};

bool emit(int type, int code, int value) {
    input_event event{};
    gettimeofday(&event.time, nullptr);
    event.type = static_cast<__u16>(type);
    event.code = static_cast<__u16>(code);
    event.value = value;
    return write(g_fd, &event, sizeof(event)) == static_cast<ssize_t>(sizeof(event));
}

bool syncFrame() { return emit(EV_SYN, SYN_REPORT, 0); }

bool setAbs(int code, int minimum, int maximum) {
    uinput_abs_setup abs{};
    abs.code = static_cast<__u16>(code);
    abs.absinfo.minimum = minimum;
    abs.absinfo.maximum = maximum;
    return ioctl(g_fd, UI_ABS_SETUP, &abs) == 0;
}

bool anyActive() {
    for (bool active : g_active) if (active) return true;
    return false;
}

bool createDevice(int width, int height) {
    g_fd = open("/dev/uinput", O_WRONLY | O_CLOEXEC);
    if (g_fd < 0) {
        std::cerr << "OPEN_UINPUT_ERR errno=" << errno << " " << strerror(errno) << std::endl;
        return false;
    }

    auto setBit = [&](unsigned long request, int bit) {
        return ioctl(g_fd, request, bit) == 0;
    };
    if (!setBit(UI_SET_EVBIT, EV_KEY) ||
        !setBit(UI_SET_KEYBIT, BTN_TOUCH) ||
        !setBit(UI_SET_KEYBIT, BTN_TOOL_FINGER) ||
        !setBit(UI_SET_EVBIT, EV_ABS) ||
        !setBit(UI_SET_ABSBIT, ABS_MT_SLOT) ||
        !setBit(UI_SET_ABSBIT, ABS_MT_TRACKING_ID) ||
        !setBit(UI_SET_ABSBIT, ABS_MT_POSITION_X) ||
        !setBit(UI_SET_ABSBIT, ABS_MT_POSITION_Y) ||
        !setBit(UI_SET_ABSBIT, ABS_MT_TOUCH_MAJOR) ||
        !setBit(UI_SET_ABSBIT, ABS_MT_PRESSURE) ||
        ioctl(g_fd, UI_SET_PROPBIT, INPUT_PROP_DIRECT) < 0) {
        std::cerr << "SET_CAPABILITIES_ERR errno=" << errno << " " << strerror(errno) << std::endl;
        close(g_fd); g_fd = -1; return false;
    }

    uinput_setup setup{};
    setup.id.bustype = BUS_VIRTUAL;
    setup.id.vendor = 0x2A7A;
    setup.id.product = 0x0002;
    setup.id.version = 1;
    std::strncpy(setup.name, "ZAY CLICKER Virtual Touchscreen", UINPUT_MAX_NAME_SIZE - 1);
    if (ioctl(g_fd, UI_DEV_SETUP, &setup) < 0) {
        std::cerr << "DEV_SETUP_ERR errno=" << errno << " " << strerror(errno) << std::endl;
        close(g_fd); g_fd = -1; return false;
    }

    if (!setAbs(ABS_MT_SLOT, 0, kSlots - 1) ||
        !setAbs(ABS_MT_TRACKING_ID, 0, 65535) ||
        !setAbs(ABS_MT_POSITION_X, 0, width - 1) ||
        !setAbs(ABS_MT_POSITION_Y, 0, height - 1) ||
        !setAbs(ABS_MT_TOUCH_MAJOR, 0, 255) ||
        !setAbs(ABS_MT_PRESSURE, 0, 255)) {
        std::cerr << "ABS_SETUP_ERR errno=" << errno << " " << strerror(errno) << std::endl;
        close(g_fd); g_fd = -1; return false;
    }

    if (ioctl(g_fd, UI_DEV_CREATE) < 0) {
        std::cerr << "DEV_CREATE_ERR errno=" << errno << " " << strerror(errno) << std::endl;
        close(g_fd); g_fd = -1; return false;
    }
    usleep(350000);
    std::cout << "READY" << std::endl;
    return true;
}

void updateTouchKeyState() {
    const int down = anyActive() ? 1 : 0;
    emit(EV_KEY, BTN_TOUCH, down);
    emit(EV_KEY, BTN_TOOL_FINGER, down);
}

void down(int slot, int x, int y) {
    if (slot < 0 || slot >= kSlots || g_active[slot]) return;
    emit(EV_ABS, ABS_MT_SLOT, slot);
    emit(EV_ABS, ABS_MT_TRACKING_ID, g_nextTrackingId++);
    emit(EV_ABS, ABS_MT_POSITION_X, x);
    emit(EV_ABS, ABS_MT_POSITION_Y, y);
    emit(EV_ABS, ABS_MT_TOUCH_MAJOR, 8);
    emit(EV_ABS, ABS_MT_PRESSURE, 80);
    g_active[slot] = true;
    updateTouchKeyState();
    syncFrame();
}

void move(int slot, int x, int y) {
    if (slot < 0 || slot >= kSlots || !g_active[slot]) return;
    emit(EV_ABS, ABS_MT_SLOT, slot);
    emit(EV_ABS, ABS_MT_POSITION_X, x);
    emit(EV_ABS, ABS_MT_POSITION_Y, y);
    syncFrame();
}

void up(int slot) {
    if (slot < 0 || slot >= kSlots || !g_active[slot]) return;
    emit(EV_ABS, ABS_MT_SLOT, slot);
    emit(EV_ABS, ABS_MT_TRACKING_ID, -1);
    g_active[slot] = false;
    updateTouchKeyState();
    syncFrame();
}

void click(int slot, int x, int y) {
    if (slot < 0 || slot >= kSlots || g_active[slot]) return;
    down(slot, x, y);
    usleep(12000);
    up(slot);
}

void releaseAll() { for (int slot = 0; slot < kSlots; ++slot) up(slot); }
}

int main(int argc, char** argv) {
    if (argc != 3) {
        std::cerr << "USAGE width height" << std::endl;
        return 2;
    }
    int width = 0, height = 0;
    try { width = std::stoi(argv[1]); height = std::stoi(argv[2]); }
    catch (...) { std::cerr << "INVALID_DISPLAY_SIZE" << std::endl; return 2; }
    if (width < 2 || height < 2 || !createDevice(width, height)) return 3;

    std::string line;
    while (std::getline(std::cin, line)) {
        std::istringstream input(line);
        std::string command;
        int slot = 0, x = 0, y = 0;
        input >> command;
        if (command == "DOWN" && input >> slot >> x >> y) down(slot, x, y);
        else if (command == "MOVE" && input >> slot >> x >> y) move(slot, x, y);
        else if (command == "UP" && input >> slot) up(slot);
        else if (command == "CLICK" && input >> slot >> x >> y) click(slot, x, y);
        else if (command == "STOP") releaseAll();
        else if (command == "QUIT") { releaseAll(); break; }
    }
    releaseAll();
    ioctl(g_fd, UI_DEV_DESTROY);
    close(g_fd);
    return 0;
}
