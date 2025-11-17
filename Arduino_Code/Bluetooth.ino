#include <SoftwareSerial.h>

SoftwareSerial BT(10, 11); // RX, TX

String inputBT = "";
String inputSerial = "";
unsigned long serialTimeout = 0;
const unsigned long SERIAL_TIMEOUT_MS = 100; // Таймаут 100мс

void messageHandler(String);

void setup() {
  Serial.begin(9600);
  BT.begin(9600);
  Serial.println("Bluetooth string communication ready!");
  
  pinMode(2, OUTPUT);
  // Встановлюємо режим AT команд для Bluetooth
  delay(1000);
  // BT.println("AT+UUID");
  // delay(1000);
  // BT.println("AT+CHAR");

}

void loop() {
  if (BT.available()) {
    char c = BT.read();

    if (c == '\n' || c == '\r') {   // кінець рядка
      inputBT.trim();

      if (inputBT.length() > 0) {
        Serial.println("Bluetooth|" + inputBT);
        messageHandler(inputBT);
      }

      inputBT = "";   // очищаємо буфер РІВНО ТУТ
    } 
    else {
      inputBT += c;   // дописуємо символи
    }
  }
}



void messageHandler(String s) {

  if (s.equals("SWITCH_LED")) {
    digitalWrite(2, !digitalRead(2));
    BT.println(digitalRead(2) ? "LED ON" : "LED OFF");
  }

  else {
    BT.println("Unknown command:" + s);
  }
}