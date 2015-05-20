// Created by Brian and Karthik for CMSC 838f final project: Spider Guider

// Pins
#define LEFTPIN1 4
#define LEFTPIN2 3
#define LEFTPIN3 2
#define FRONTPIN1 7
#define FRONTPIN2 6
#define FRONTPIN3 5
#define RIGHTPIN1 12
#define RIGHTPIN2 11
#define RIGHTPIN3 10

#define LEFTSIDE 0
#define FRONTSIDE 1
#define RIGHTSIDE 2

//Bluetooth
#include <SoftwareSerial.h>

int bluetoothTx = 8;  // TX-O pin of bluetooth mate, Arduino D2
int bluetoothRx = 9;  // RX-I pin of bluetooth mate, Arduino D3

SoftwareSerial bluetooth(bluetoothTx, bluetoothRx);

int left = 0; 
int right = 0; 
int front = 0; 
int leftDistance = 0; 
int rightDistance = 0; 
int frontDistance = 0; 


int distL, distS, distR, totalL, totalS, totalR;
float normTotalL, normTotalS, normTotalR;

int motors[3][3] = {
  {LEFTPIN1, LEFTPIN2, LEFTPIN3},
  {FRONTPIN1, FRONTPIN2, FRONTPIN3},
  {RIGHTPIN1, RIGHTPIN2, RIGHTPIN3}
};

void setup() {
  // Open serial port
  Serial.begin(9600);  
  
  pinMode(LEFTPIN1, OUTPUT);
  pinMode(LEFTPIN2, OUTPUT);
  pinMode(LEFTPIN3, OUTPUT);
  pinMode(FRONTPIN1, OUTPUT);
  pinMode(FRONTPIN2, OUTPUT);
  pinMode(FRONTPIN3, OUTPUT);
  pinMode(RIGHTPIN1, OUTPUT);
  pinMode(RIGHTPIN2, OUTPUT);
  pinMode(RIGHTPIN3, OUTPUT);
  
  bluetooth.begin(115200);  // The Bluetooth Mate defaults to 115200bps
  bluetooth.print("$");  // Print three times individually
  bluetooth.print("$");
  bluetooth.print("$");  // Enter command mode
  delay(100);  // Short delay, wait for the Mate to send back CMD
  bluetooth.println("U,9600,N");  // Temporarily Change the baudrate to 9600, no parity
  // 115200 can be too fast at times for NewSoftSerial to relay the data reliably
  bluetooth.begin(9600);  // Start bluetooth serial at 9600
}



void loop() {
  
  //Alert();
  
  while (true) {
  
    int total = bluetooth.available(); 
  
    if (total > 10) {
      
      char message[total+1];
      
      int i = 0; 
      
      while (bluetooth.available())  {
        
        // Send any characters the bluetooth prints to the serial monitor
        char d = (char)bluetooth.read(); 
        
        message[i] = d; 
        
        if (d != ',') {
            if (i == 0) {
              left = d - '0';
            }
           
            if (i == 2) {
              right = d - '0';
            }
          
            if (i == 4) {
              front = d - '0';
            }    
            
            if (i == 6) {
              leftDistance = d - '0';
            }
           
            if (i == 8) {
              rightDistance = d - '0';
            }
          
            if (i == 10) {
              frontDistance = d - '0';
            }
        }
        
        i++;         
      } 
      
      left = left < 0? 0: left;
      right = right < 0? 0: right;
      front = front < 0? 0: front; 
      
      leftDistance = leftDistance < 0? 0: leftDistance;
      rightDistance = rightDistance < 0? 0: rightDistance;
      frontDistance = frontDistance < 0? 0: frontDistance; 
          
      message[i] = '\0';
      Serial.print((String) left);
      Serial.print(",");
      Serial.print((String) right);
      Serial.print(",");
      Serial.print((String) front);
      Serial.print(",");
      Serial.print((String) leftDistance);
      Serial.print(",");
      Serial.print((String) rightDistance);
      Serial.print(",");
      Serial.println((String) frontDistance);
 
      BuzzControl();
      
      break;
    } 
  }
  
  //testCode();
  
}


void testCode() {
  
   digitalWrite(2, HIGH);
   delay(2000); 
   digitalWrite(2, LOW);
   delay(200); 
   
   digitalWrite(3, HIGH);
   delay(2000); 
   digitalWrite(3, LOW);
   delay(200); 
  
   digitalWrite(4, HIGH);
   delay(2000); 
   digitalWrite(4, LOW);
   delay(200); 
  
  digitalWrite(5, HIGH);
   delay(2000); 
   digitalWrite(5, LOW);
   delay(200); 
  
   digitalWrite(6, HIGH);
   delay(2000); 
   digitalWrite(6, LOW);
   delay(200); 
   
   digitalWrite(7, HIGH);
   delay(2000); 
   digitalWrite(7, LOW);
   delay(200); 
   
   digitalWrite(10, HIGH);
   delay(2000); 
   digitalWrite(10, LOW);
   delay(200); 
   
   digitalWrite(11, HIGH);
   delay(2000); 
   digitalWrite(11, LOW);
   delay(200); 
   
   digitalWrite(12, HIGH);
   delay(2000); 
   digitalWrite(12, LOW);
   delay(200); 
    
}

//void ReadBlue() {
//  // Set distL, distS, distR, totalL, totalS, totalR
//  
//  
//}


void BuzzControl() {
  //Alert ();
  
  totalL = left;
  totalS = front;
  totalR = right;
  distL = leftDistance;
  distS = frontDistance;
  distR = rightDistance;
  
  
  NormTotals();
//  RoundDist();
  
  int inputVals[3][2] = {
    {distL, totalL},
    {distS, totalS},
    {distR, totalR}
  };
  
  int outputVals[3][3]= {
    {0, distL, totalL},
    {1, distS, totalS},
    {2, distR, totalR}
  };
  
  for (int i = 0; i < 3; i++) {
    if (inputVals[i][0] < outputVals[0][1]) {
      outputVals[0][0] = i;
      outputVals[0][1] = inputVals[i][0];
      outputVals[0][2] = inputVals[i][1];
    }
    if (inputVals[i][0] > outputVals[2][1]) {
      outputVals[2][0] = i;
      outputVals[2][1] = inputVals[i][0];
      outputVals[2][2] = inputVals[i][1];
    }
  }
  
  // find middle
  for (int i = 0; i < 3; i++) {
    if (i != outputVals[0][0] && i != outputVals[2][0])
      outputVals[1][0] = i;
      outputVals[1][1] = inputVals[i][0];
      outputVals[1][2] = inputVals[i][1];
  }
  
  for (int i = 0; i < 3; i++) {
    Vibe(outputVals[i][0], outputVals[i][1], outputVals[i][2]);
  }
  
}


void NormTotals() {
  int sum;
  
  sum = totalL + totalS + totalR;
  
  totalL = 6*((float)(totalL+2)/(float)sum);
  if (totalL > 3)
    totalL = 3;
    
  totalS = 6*((float)(totalS+2)/(float)sum);
  if (totalS > 3)
    totalS = 3;
    
  totalR = 6*((float)(totalR+2)/(float)sum);
  if (totalR > 3)
    totalR = 3;
}


void Vibe(int dir, int numBuzz, int level) {
  if (level < 0)
    level = 0;
  
  if (level > 3)
    level =3;
    
  for (int i = 0; i < numBuzz; i++) {
    for (int j = 0; j < level; j++)
      digitalWrite(motors[dir][j], HIGH);
    delay(300);
    for (int j = 0; j < level; j++)
      digitalWrite(motors[dir][j], LOW);
    delay(200);
  }
}


void Alert() {
  for (int j = 0; j < 3; j++) {
    for (int i = 0; i < 3; i++)
      digitalWrite(motors[i][j], HIGH);
    delay(300);
    for (int i = 0; i < 3; i++)
      digitalWrite(motors[i][j], LOW);
  }
  delay(500);

  for (int j = 0; j < 3; j++) {
    for (int i = 0; i < 3; i++)
      digitalWrite(motors[i][j], HIGH);
    delay(300);
    for (int i = 0; i < 3; i++)
      digitalWrite(motors[i][j], LOW);
  }
  delay(500);
}


void ErrorVibe() {
  for (int i = 0; i < 3; i++) {
    for (int j = 0; j < 3; j++)
      digitalWrite(motors[i][j], HIGH);
  }
  delay(1000);
  for (int i = 0; i < 3; i++) {
    for (int j = 0; j < 3; j++)
      digitalWrite(motors[i][j], LOW);
  }
  delay(1000);
}





