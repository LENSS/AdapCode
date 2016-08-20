interface CC2420PacketDefer {
async event void deferSend(uint16_t duration);
async event void setNoSend(uint16_t duration);
}
