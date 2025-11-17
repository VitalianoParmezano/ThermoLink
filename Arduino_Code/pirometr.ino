#include <Wire.h>

#define MLX_ADDR 0x5A

// Читання сирого 16-біт значення + ігнорування PEC
uint16_t read16(uint8_t reg) {
  Wire.beginTransmission(MLX_ADDR);
  Wire.write(reg);
  Wire.endTransmission(false); // repeated start

  Wire.requestFrom(MLX_ADDR, 3);

  uint8_t lsb = Wire.read(); 
  uint8_t msb = Wire.read();
  uint8_t pec = Wire.read(); // PEC поки ігноруємо

  return (msb << 8) | lsb;
}

// Конвертація RAW -> °C
float readTempC(uint8_t reg) {
  uint16_t raw = read16(reg);
  if (raw == 0) return NAN;

  return (raw * 0.02) - 273.15;
}

float readAmbient() {
  return readTempC(0x06);
}

float readObject() {
  return readTempC(0x07);
}

void setup() {
  Serial.begin(9600);
  Wire.begin();
}

void loop() {
  Serial.print("Ambient: ");
  Serial.print(readAmbient());
  Serial.print(" C   Object: ");
  Serial.print(readObject());
  Serial.println(" C");

  delay(500);
}
