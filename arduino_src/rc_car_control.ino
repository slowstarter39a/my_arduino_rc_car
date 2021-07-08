// BT : HC-06
// Motor Driver : L9110S motor driver
// Motor : DC motor

#include <SoftwareSerial.h>
// HC-06 connection
const int BT_RXD = 13;
const int BT_TXD = 12;

const int GET_VERSION = 0;
const int DRIVE = 1;

// L9110 motor driver connection
const int front_a1 = 3;
const int front_a2 = 5;
const int front_b1 = 6;
const int front_b2 = 9;

int bufPos = 0;
byte dataBuf[100] = {0,};
char pktHead[] = "mycar";

bool isMyPkt(char buf[])
{
  int n = sizeof(pktHead) - 1; //except null 
  
  for (int i = 0; i < n; i++) {
    if (buf[i] != pktHead[i]) {
      return false;
    }
  }
  return true;
}

SoftwareSerial my_hc06(BT_RXD, BT_TXD);


void setup() {
  Serial.begin(9600);
  Serial.println("Enter AT Commands:");
    
  my_hc06.begin(9600);
    
  pinMode(front_a1, OUTPUT);
  pinMode(front_a2, OUTPUT);
  pinMode(front_b1, OUTPUT);
  pinMode(front_b2, OUTPUT);
}

void drive(int front_left, int front_right)
{
  if (front_left <= 0) {
    analogWrite(front_a1, 0);
    analogWrite(front_a2, -front_right);
    analogWrite(front_b1, -front_left);
    analogWrite(front_b2, 0);
  }
  
  else {
    analogWrite(front_a1, front_left);
    analogWrite(front_a2, 0);
    analogWrite(front_b1, 0);
    analogWrite(front_b2, front_right); 
  }
  
}

void dispatch_command(byte buf[])
{
  //buf[0 ~ 4] : pkt head "mycar"
  //buf[5]     : pkt length;
  //buf[6]     : cmd;
  //buf[7 ~]]  : data
  //buf[last]  : '\n'
  if (!isMyPkt(buf)) {
    return;
  }
  
  switch(buf[6]) {
    case GET_VERSION:
      break;
      
    case DRIVE :
      int front_left = buf[7] | (buf[8] << 8);
      int front_right = buf[9] | (buf[10] << 8);
        
      drive(front_left, front_right);
                    
      break;
      
    default:
      break;    
  }
  
}

void loop() {
  while (my_hc06.available()) {
    dataBuf[bufPos] = (byte)my_hc06.read();
    if ( dataBuf[bufPos] == '\n') {
      dispatch_command(dataBuf);
      bufPos = 0;
    }
    else {
      bufPos++;
    }
  }  
  
  if (Serial.available()) {
    my_hc06.write(Serial.read());
  }    
}
