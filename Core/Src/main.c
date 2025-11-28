/* USER CODE BEGIN Header */
/**
  ******************************************************************************
  * @file           : main.c
  * @brief          : Main program body
  ******************************************************************************
  * @attention
  *
  * Copyright (c) 2025 STMicroelectronics.
  * All rights reserved.
  *
  * This software is licensed under terms that can be found in the LICENSE file
  * in the root directory of this software component.
  * If no LICENSE file comes with this software, it is provided AS-IS.
  *
  ******************************************************************************
  */
/* USER CODE END Header */
/* Includes ------------------------------------------------------------------*/
#include "main.h"
#include "i2c.h"
#include "usart.h"
#include "gpio.h"

/* Private includes ----------------------------------------------------------*/
/* USER CODE BEGIN Includes */
#include <string.h>
#include <stdio.h>
/* USER CODE END Includes */

/* Private typedef -----------------------------------------------------------*/
/* USER CODE BEGIN PTD */

/* USER CODE END PTD */

/* Private define ------------------------------------------------------------*/
/* USER CODE BEGIN PD */
#define LED_PIN GPIO_PIN_13
#define LED_PORT GPIOD

#define MLX_ADDR 0x5A << 1  // I2C адреса MLX90614
/* USER CODE END PD */

/* Private macro -------------------------------------------------------------*/
/* USER CODE BEGIN PM */

/* USER CODE END PM */

/* Private variables ---------------------------------------------------------*/

/* USER CODE BEGIN PV */
/// UART_HandleTypeDef huart4;  - оголошено в usart.h

// Bluetooth reception buffers
char bt_rx_buffer[1];           // Single character receive buffer
char bt_input_buffer[128];      // Complete message buffer
uint16_t bt_input_index = 0;    // Current position in input buffer
uint8_t bt_message_ready = 0;   // Flag indicating complete message received

// Pirometr
uint32_t last_temp_measurement = 0;
#define TEMP_MEASURE_INTERVAL 1000  // 1 секунда
/* USER CODE END PV */

/* Private function prototypes -----------------------------------------------*/
void SystemClock_Config(void);
/* USER CODE BEGIN PFP */
void BT_Init(void);                           // Initialize Bluetooth communication
void BT_MessageHandler(char *message);        // Handle specific Bluetooth commands
void BT_SendMessage(char *message);           // Send message via Bluetooth

uint16_t MLX_Read_Register(uint8_t reg);
float MLX_ReadTempAmbient(void);
float MLX_ReadTempObject(void);
/* USER CODE END PFP */

/* Private user code ---------------------------------------------------------*/
/* USER CODE BEGIN 0 */

/* USER CODE END 0 */

/**
  * @brief  The application entry point.
  * @retval int
  */
int main(void)
{

  /* USER CODE BEGIN 1 */

  /* USER CODE END 1 */

  /* MCU Configuration--------------------------------------------------------*/

  /* Reset of all peripherals, Initializes the Flash interface and the Systick. */
  HAL_Init();

  /* USER CODE BEGIN Init */

  /* USER CODE END Init */

  /* Configure the system clock */
  SystemClock_Config();

  /* USER CODE BEGIN SysInit */

  /* USER CODE END SysInit */

  /* Initialize all configured peripherals */
  MX_GPIO_Init();
  MX_UART4_Init();
  MX_I2C1_Init();
  /* USER CODE BEGIN 2 */
  BT_Init();
  /* USER CODE END 2 */

  /* Infinite loop */
  /* USER CODE BEGIN WHILE */
  while (1)
  {

	    if (bt_message_ready!=0) {
	    	BT_MessageHandler(bt_input_buffer);

	    	HAL_Delay(200);


	    }
    /* USER CODE END WHILE */

    /* USER CODE BEGIN 3 */



  }
  /* USER CODE END 3 */
}

/**
  * @brief System Clock Configuration
  * @retval None
  */
void SystemClock_Config(void)
{
  RCC_OscInitTypeDef RCC_OscInitStruct = {0};
  RCC_ClkInitTypeDef RCC_ClkInitStruct = {0};

  /** Configure the main internal regulator output voltage
  */
  __HAL_RCC_PWR_CLK_ENABLE();
  __HAL_PWR_VOLTAGESCALING_CONFIG(PWR_REGULATOR_VOLTAGE_SCALE1);

  /** Initializes the RCC Oscillators according to the specified parameters
  * in the RCC_OscInitTypeDef structure.
  */
  RCC_OscInitStruct.OscillatorType = RCC_OSCILLATORTYPE_HSI;
  RCC_OscInitStruct.HSIState = RCC_HSI_ON;
  RCC_OscInitStruct.HSICalibrationValue = RCC_HSICALIBRATION_DEFAULT;
  RCC_OscInitStruct.PLL.PLLState = RCC_PLL_ON;
  RCC_OscInitStruct.PLL.PLLSource = RCC_PLLSOURCE_HSI;
  RCC_OscInitStruct.PLL.PLLM = 8;
  RCC_OscInitStruct.PLL.PLLN = 144;
  RCC_OscInitStruct.PLL.PLLP = RCC_PLLP_DIV2;
  RCC_OscInitStruct.PLL.PLLQ = 4;
  if (HAL_RCC_OscConfig(&RCC_OscInitStruct) != HAL_OK)
  {
    Error_Handler();
  }

  /** Initializes the CPU, AHB and APB buses clocks
  */
  RCC_ClkInitStruct.ClockType = RCC_CLOCKTYPE_HCLK|RCC_CLOCKTYPE_SYSCLK
                              |RCC_CLOCKTYPE_PCLK1|RCC_CLOCKTYPE_PCLK2;
  RCC_ClkInitStruct.SYSCLKSource = RCC_SYSCLKSOURCE_PLLCLK;
  RCC_ClkInitStruct.AHBCLKDivider = RCC_SYSCLK_DIV1;
  RCC_ClkInitStruct.APB1CLKDivider = RCC_HCLK_DIV4;
  RCC_ClkInitStruct.APB2CLKDivider = RCC_HCLK_DIV2;

  if (HAL_RCC_ClockConfig(&RCC_ClkInitStruct, FLASH_LATENCY_4) != HAL_OK)
  {
    Error_Handler();
  }
}

/* USER CODE BEGIN 4 */
void BT_MessageHandler(char *message)
{
  // SWITCH_LED command - toggle the LED state
  if (strcmp(message, "SWITCH_LED") == 0) {
    // Toggle LED pin (like digitalWrite in Arduino)
    HAL_GPIO_TogglePin(LED_PORT, LED_PIN);

    // Send response back via Bluetooth with current LED state
    if (HAL_GPIO_ReadPin(LED_PORT, LED_PIN)) {
      BT_SendMessage("LED ON");
    } else {
      BT_SendMessage("LED OFF");
    }
  }
  else if (strcmp(message, "PIROMETR") == 0) {
	  float ambient = MLX_ReadTempAmbient();
	  float object = MLX_ReadTempObject();

	  // Конвертація у цілі числа (наприклад, помножені на 10 для 1 десяткового знака)
	  int ambient_int = (int)(ambient * 10);
	  int object_int = (int)(object * 10);

	  char temp_msg[64];
	  sprintf(temp_msg, "pirometr%d.%d|%d.%d",
			  ambient_int / 10, abs(ambient_int % 10),
			  object_int / 10, abs(object_int % 10));

  	  BT_SendMessage(temp_msg);
  }
  else if (strcmp(message,"THERMOPAIR") == 0){
	  char temp_msg[64];
	  sprintf(temp_msg, "Thermopair is not ready");
  	  BT_SendMessage(temp_msg);
  }
  // Unknown command handler
  else {
    char response[50];
    sprintf(response, "Unknown command: %s", message);
    BT_SendMessage(response);
  }

  bt_message_ready = 0;           // Clear the message ready flag
  bt_input_index = 0;             // Reset buffer position
  memset(bt_input_buffer, 0, sizeof(bt_input_buffer));  // Clear buffer contents

}

// My system code
void BT_Init(void)
{
  // Start receiving data via UART interrupt
  // When data arrives, HAL_UART_RxCpltCallback will be called automatically
  HAL_UART_Receive_IT(&huart4, (uint8_t*)bt_rx_buffer, 1);

  // Clear the input buffer to ensure it's empty
  memset(bt_input_buffer, 0, sizeof(bt_input_buffer));

  // Reset buffer position and message flag
  bt_input_index = 0;
  bt_message_ready = 0;
}
// Переривання по Юарт
void HAL_UART_RxCpltCallback(UART_HandleTypeDef *huart)
{
  // Check if this interrupt came from our Bluetooth UART
  if (huart->Instance == huart4.Instance) {
    char received_char = bt_rx_buffer[0];

    // Check for end-of-line characters (like Arduino Serial.read())
    if (received_char == '\n' || received_char == '\r') {
      // If we have data in buffer, mark message as ready for processing
      if (bt_input_index > 0) {
        bt_message_ready = 1;
      }
    }
    else {
      // Add character to buffer if there's space
      if (bt_input_index < sizeof(bt_input_buffer) - 1) {
        bt_input_buffer[bt_input_index++] = received_char;
      }
      // If buffer is full, you might want to handle overflow here
    }

    // Restart interrupt-based reception for next character
    // This is IMPORTANT - without this, you'll only receive one character!
    HAL_UART_Receive_IT(&huart4, (uint8_t*)bt_rx_buffer, 1);
  }
}

void BT_SendMessage(char *message)
{
  char buffer[128];

  // Format message with line ending (like Serial.println in Arduino)
  sprintf(buffer, "%s\r\n", message);

  // Transmit message via UART5 (Bluetooth)
  // HAL_MAX_DELAY means wait forever if needed
  HAL_UART_Transmit(&huart4, (uint8_t*)buffer, strlen(buffer), HAL_MAX_DELAY);
}

/*
Пірометр код нижче
*/

float MLX_ReadTempAmbient(void)
{
    uint16_t raw = MLX_Read_Register(0x06); // 0x06 - ambient temperature
    if (raw == 0) return 0;
    return (raw * 0.02f) - 273.15f;
}

float MLX_ReadTempObject(void)
{
    uint16_t raw = MLX_Read_Register(0x07); // 0x07 - object temperature (виправити з 0x06 на 0x07!)
    if (raw == 0) return 0;
    return (raw * 0.02f) - 273.15f;
}

uint16_t MLX_Read_Register(uint8_t reg)
{
    uint8_t data[3];
    HAL_StatusTypeDef status;

    // Використовуємо HAL_I2C_Mem_Read для автоматичного repeated start
    status = HAL_I2C_Mem_Read(&hi2c1, MLX_ADDR, reg, I2C_MEMADD_SIZE_8BIT, data, 3, 100);

    if (status != HAL_OK) {
        // Спроба ще раз з більшим таймаутом
        status = HAL_I2C_Mem_Read(&hi2c1, MLX_ADDR, reg, I2C_MEMADD_SIZE_8BIT, data, 3, 500);
        if (status != HAL_OK) {
            return 0;
        }
    }

    // data[0] - LSB, data[1] - MSB, data[2] - PEC
    uint16_t result = (data[1] << 8) | data[0];

    return result;
}
/* USER CODE END 4 */

/**
  * @brief  This function is executed in case of error occurrence.
  * @retval None
  */
void Error_Handler(void)
{
  /* USER CODE BEGIN Error_Handler_Debug */
  /* User can add his own implementation to report the HAL error return state */
  __disable_irq();
  while (1)
  {
  }
  /* USER CODE END Error_Handler_Debug */
}
#ifdef USE_FULL_ASSERT
/**
  * @brief  Reports the name of the source file and the source line number
  *         where the assert_param error has occurred.
  * @param  file: pointer to the source file name
  * @param  line: assert_param error line source number
  * @retval None
  */
void assert_failed(uint8_t *file, uint32_t line)
{
  /* USER CODE BEGIN 6 */
  /* User can add his own implementation to report the file name and line number,
     ex: printf("Wrong parameters value: file %s on line %d\r\n", file, line) */
  /* USER CODE END 6 */
}
#endif /* USE_FULL_ASSERT */
