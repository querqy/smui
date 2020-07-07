package models

object Status {

  def isActiveFromStatus(status: Int): Boolean = {
    (status & 0x01) == 0x01
  }

  def statusFromIsActive(isActive: Boolean): Int = {
    if (isActive) 0x01 else 0x00
  }

}
