#ifndef RADIO_COUNT_TO_LEDS_H
#define RADIO_COUNT_TO_LEDS_H

typedef nx_struct radio_data_msg {
  nx_uint8_t start_serial;
  nx_uint8_t end_serial;
  nx_uint16_t coeff;
  nx_uint8_t data[TOSH_DATA_LENGTH - 4];
} radio_data_msg_t;

typedef nx_struct radio_nack_msg {
  nx_uint8_t start_serial;
  nx_uint8_t missing;
} radio_nack_msg_t;

enum {
  AM_RADIO_DATA_MSG = 6,
  AM_RADIO_NACK_MSG = 7,
};

#define SOURCE 50
#define TOTAL_PKT 128
#define QUEUE_SIZE 64


#endif
