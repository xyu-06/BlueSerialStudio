"""Static regression checks for dynamically built Android UI, not a device test."""
from pathlib import Path
p=Path(__file__).resolve().parents[1] / 'app/src/main/java/com/embedded/blueserial/MainActivity.java'
s=p.read_text()
assert 'sendSuffixLabel.setText' in s, 'suffix hint not updated'
assert 'refreshSendSuffix();' in s and 'onItemSelected' in s, 'suffix options not synchronized'
assert 'WindowInsets.Type.ime()' in s, 'keyboard insets not handled'
assert 'page.addView(dock' in s, 'send panel not docked'
assert 'new AxisJoystickView(this, AxisJoystickView.Axis.VERTICAL)' in s
assert 'new AxisJoystickView(this, AxisJoystickView.Axis.HORIZONTAL)' in s
assert 'sendDriveState(steering, throttle, released);' in s
assert 'macros.add(new MacroEntry' in s and 'macros.remove(existing)' in s
assert 'sliders.add(new SliderEntry' in s and 'sliders.remove(existing)' in s
assert 'saveMacros()' in s and 'saveSliders()' in s
assert 'putString(MACRO_STORE' in s and 'putString(SLIDER_STORE' in s
assert 'clearMonitoring()' in s and 'transmittedCount = 0;' in s and 'receivedCount = 0;' in s
assert 'new JoystickView(this)' in s and 'new AxisJoystickView(this, AxisJoystickView.Axis.VERTICAL)' in s
assert 'controlMode.setOnItemSelectedListener' in s and 'preferences.edit().putInt("control_mode", mode)' in s
assert 'bindDirectionButton(forward, 0)' in s and 'bindDirectionButton(right, 3)' in s
assert 'sendButtonMotion(false)' in s and 'DriveControlMath.buttonAxis' in s
assert 'sendDriveState(steering, throttle, released)' in s
assert 'sendDriveState(0, 0, true)' in s
assert 'sendDriveCommand(' not in s and 'sendCombinedJoystick(' not in s
assert 'boolean hexMode;' in s
assert 'o.optBoolean("hex", false)' in s, 'old shortcut settings must default to text'
assert 'o.put("hex", m.hexMode)' in s, 'shortcut HEX setting must persist'
assert 'macroMode.setSelection(existing != null && existing.hexMode ? 1 : 0)' in s
assert 'sendMacro(item)' in s and 'SerialProtocol.parseHex(macro.command)' in s
assert 'HEX 原始字节（不添加结束符）' in s
assert 'pages = new View[5]' in s and 'navButtons = new Button[5]' in s
assert 'pages[4] = createAtToolPage()' in s and '"HC-05 AT"' in s
assert 'createAtToolPage()' in s and 'addAtCommandRow(quick, "查询波特率"' not in s
assert 'copyAtCommand(command)' in s and 'AtCommandBuilder.withCrlf(command)' in s
assert '不会通过手机蓝牙 SPP 发送 AT 指令' in s
assert '上电后红灯会先快闪约 3 秒' in s and '38400,1,0' in s
assert 'AT+ORGL' in s and 'AT+RESET' in s and 'AT+UART?' in s
print('35 UI/source static checks passed')
