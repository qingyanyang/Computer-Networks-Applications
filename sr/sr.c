#include <stdlib.h>
#include <stdio.h>
#include <stdbool.h>
#include <string.h>
#include "emulator.h"
#include "sr.h"

/* ******************************************************************
   Go Back N protocol.  Adapted from J.F.Kurose
   ALTERNATING BIT AND GO-BACK-N NETWORK EMULATOR: VERSION 1.2

   Network properties:
   - one way network delay averages five time units (longer if there
   are other messages in the channel for GBN), but can be larger
   - packets can be corrupted (either the header or the data portion)
   or lost, according to user-defined probabilities
   - packets will be delivered in the order in which they were sent
   (although some can be lost).

   Modifications:
   - removed bidirectional GBN code and other code not used by prac.
   - fixed C style to adhere to current programming style
   - added GBN implementation
**********************************************************************/

#define RTT  16.0       /* round trip time.  MUST BE SET TO 16.0 when submitting assignment */
#define WINDOWSIZE 6    /* the maximum number of buffered unacked packet */
#define SEQSPACE (2*WINDOWSIZE)    /* the min sequence space for SR must be at least windowsize * 2 */
#define NOTINUSE (-1)   /* used to fill header fields that are not being used */

/* generic procedure to compute the checksum of a packet.  Used by both sender and receiver
   the simulator will overwrite part of your packet with 'z's.  It will not overwrite your
   original checksum.  This procedure must generate a different checksum to the original if
   the packet is corrupted.
*/
int ComputeChecksum(struct pkt packet)
{
  int checksum = 0;
  int i;

  checksum = packet.seqnum;
  checksum += packet.acknum;
  for ( i=0; i<20; i++ )
    checksum += (int)(packet.payload[i]);

  return checksum;
}

int IsCorrupted(struct pkt packet)
{
  if (packet.checksum == ComputeChecksum(packet))
    return -1;
  else
    return 0;
}

/********* Sender (A) variables and functions ************/
static struct pkt buffer[WINDOWSIZE]; /* array for storing packets waiting for ACK */
static int baseseqnum_a; /*record the first seq num of the window*/
static int windowcount;  /* the number of packets currently awaiting an ACK */
static int A_nextseqnum; /* the next sequence number to be used by the sender */

/* 0. Here only sequential transmission is considered, without considering retransmission, retransmission is sent in the function timeout, the mechanism here is?? The principle is that whichever packet times out, send that one.
1. Send in order (a_nextseqnum can determine which packet to send this time).
2. The receiver window moves faster than the sender window, so the packet to be sent can only be within the window.
3. If a_nextseqnum is within [baseseqnum, (baseseqnum+N)%seqspace], it means that there are unsent packets in the window. If this is the case, send them. Otherwise, it means the window is full and needs to wait.
4. The packet is sent regardless of the window movement. The window only moves when the acked packet is received.
5. It is necessary to consider marking the oldest one which has not received acked, which is actually the sequence number corresponding to windowfirst.
6. Timer needs to be started for the first packet sent.
7. Send one, buffer one. */

/* called from layer 5 (application layer), passed the message to be sent to other side */
void A_output(struct msg message)
{
  struct pkt sendpkt;
  int i;
  int index;
  int seqfirst = baseseqnum_a;
  int seqlast = (baseseqnum_a + WINDOWSIZE-1) % SEQSPACE;

  /* if the A_nextseqnum is inside the window */
  if (((seqfirst <= seqlast) && (A_nextseqnum >= seqfirst && A_nextseqnum <= seqlast)) ||
      ((seqfirst > seqlast) && (A_nextseqnum >= seqfirst || A_nextseqnum <= seqlast)))
  {
    if (TRACE > 1)
      printf("----A: New message arrives, send window is not full, send new messge to layer3!\n");

    /* create packet */
    sendpkt.seqnum = A_nextseqnum;
    sendpkt.acknum = NOTINUSE;
    for (i = 0; i < 20; i++)
      sendpkt.payload[i] = message.data[i];
    sendpkt.checksum = ComputeChecksum(sendpkt);

    /* put packet in window buffer */
    if (A_nextseqnum >= seqfirst)
      index = A_nextseqnum - seqfirst;
    else
      index = WINDOWSIZE - seqfirst + A_nextseqnum;
    buffer[index] = sendpkt;
    windowcount++;

    /* send out packet */
    if (TRACE > 0)
      printf("Sending packet %d to layer 3\n", sendpkt.seqnum);
    tolayer3(A, sendpkt);

    /* start timer if first packet in window */
    if (A_nextseqnum == seqfirst)
      starttimer(A, RTT);

    /* get next sequence number, wrap back to 0 */
    A_nextseqnum = (A_nextseqnum + 1) % SEQSPACE;

  }
  /* if blocked,  window is full */
  else
  {
    if (TRACE > 0)
      printf("----A: New message arrives, send window is full\n");
    window_full++;
  }
}

/* called from layer 3, when a packet arrives for layer 4
   In this practical this will always be an ACK as B never sends data.
*/
/*0. Upon receiving a packet, first check if it is corrupted. If all packets are normal, mark this packet as "buffered ok" in this window.
1. Check if the received ack is the oldest one. If it is, restart the timer and move the window forward (by the number of consecutive acks received following the current one).
2. If the ack received is not for the oldest packet, first buffer the ack (in a new buffer, not the one used for storing packets). Only when the ack is buffered, can we determine how many windows to move.
3. How to check? Check the seqnum! Take the seqnum of the packet stored in the buffer[windowfirst], and compare it with the acknum of the received ack. If they are equal (which won't be less than, as the receiver window moves faster than the sender), then it is the oldest one.
4. Still need to handle duplicates, because the timeout may be set very short, and when a timeout occurs, the packet may be resent, and after a while, an ack is received, but the window has already moved forward.*/
void A_input(struct pkt packet)
{
  int ackcount = 0;
  int i;
  int seqfirst;
  int seqlast;
  int index;
  /* if received ACK is not corrupted */
if (IsCorrupted(packet) == -1)
{
  if (TRACE > 0)
    printf("----A: uncorrupted ACK %d is received\n", packet.acknum);
  total_ACKs_received++;

  /* need to check if new ACK or duplicate */
  seqfirst = baseseqnum_a;
  seqlast = (baseseqnum_a + WINDOWSIZE - 1) % SEQSPACE;

  /* check case when seqnum has and hasn't wrapped */
  if (((seqfirst <= seqlast) && (packet.acknum >= seqfirst && packet.acknum <= seqlast)) ||
      ((seqfirst > seqlast) && (packet.acknum >= seqfirst || packet.acknum <= seqlast)))
  {
    /* check coresponding position in window buffer */
    if (packet.acknum >= seqfirst)
      index = packet.acknum - seqfirst;
    else
      index = WINDOWSIZE - seqfirst + packet.acknum;

    if (buffer[index].acknum == NOTINUSE)
    {
      /* packet is a new ACK */
      if (TRACE > 0)
        printf("----A: ACK %d is not a duplicate\n", packet.acknum);
      new_ACKs++;
      windowcount--;
      buffer[index].acknum = packet.acknum;
    }
    else
    {
      if (TRACE > 0)
        printf("----A: duplicate ACK received, do nothing!\n");
    }
    /*compare
    it is the base one*/
    if (packet.acknum == seqfirst)
    {
      /*check how many concsecutive acks received in buffer*/
      for (i = 0; i < WINDOWSIZE; i++)
      {
        if (buffer[i].acknum != NOTINUSE && strcmp(buffer[i].payload, "") != 0)
          ackcount++;
        else
          break;
      }

      /*slide window*/
      baseseqnum_a = (baseseqnum_a + ackcount) % SEQSPACE;

      /*update buffer*/
      for (i = 0; i < WINDOWSIZE; i++)
      {
        if (buffer[i + ackcount].acknum == NOTINUSE || (buffer[i].seqnum + ackcount) % SEQSPACE == A_nextseqnum)
          buffer[i] = buffer[i + ackcount];
      }

      /*restart timer*/
      stoptimer(A);
      if (windowcount > 0)
        starttimer(A, RTT);
    }
    else
    {
      /*update buffer*/
      buffer[index].acknum = packet.acknum;
    }
  }
  }
  else
  {
    if (TRACE > 0)
      printf("----A: corrupted ACK is received, do nothing!\n");
  }
}

/* called when A's timer goes off */
/* When it is necessary to resend a packet, the oldest unacknowledged packet should be resent*/
void A_timerinterrupt(void)
{
  if (TRACE > 0)
  {
    printf("----A: time out,resend packets!\n");
    printf("---A: resending packet %d\n", (buffer[0]).seqnum);
  }
  tolayer3(A, buffer[0]);
  packets_resent++;
  starttimer(A, RTT);
}


/* the following routine will be called once (only) before any other */
/* entity A routines are called. You can use it to do any initialization */
void A_init(void)
{
  /* initialise A's window, buffer and sequence number */
  A_nextseqnum = 0;  /* A starts with seq num 0, do not change this */
  baseseqnum_a=0; /*record the first seq num of the window*/
  windowcount=0;
}


/********* Receiver (B)  variables and procedures ************/

static struct pkt buffer_b[WINDOWSIZE];    /* array for storing packets waiting for packet from A */
static int baseseqnum_b;        /*record the first seq num of the receiver's window*/
static int receivelast; /*record the last packet received position*/
/* called from layer 3, when a packet arrives for layer 4 at B*/
/*
0. Upon receiving a packet, the receiver first checks if the packet is corrupted.
1. If the received packet is within the receiver's window and not a duplicate, it is marked as received in the window buffer and an ACK is sent back to the sender.
2. Check if the received packet is the first one in the window. If it is, move the window forward (by the number of currently received packets plus any additional consecutive packets that have been acknowledged) and store the packet in the window buffer.
3. If the received packet is not the first one in the window, store it temporarily.
4. The check involves verifying whether the sequence number of the received packet matches the sequence number of the first packet in the window.
5. If the received packet is not within the window or is a duplicate, it is not stored in the window buffer, but an ACK is still sent back to the sender.
*/

void B_input(struct pkt packet)
{
  int pckcount = 0;
  struct pkt sendpkt;
  int i;
  int seqfirst;
  int seqlast;
  int index;
  /* if received packet is not corrupted */
  if (IsCorrupted(packet)==-1)
  {
    if (TRACE > 0)
      printf("----B: packet %d is correctly received, send ACK!\n", packet.seqnum);
    packets_received++;
    /*create sendpkt*/
    /* send an ACK for the received packet */
    sendpkt.acknum = packet.seqnum;
    sendpkt.seqnum = NOTINUSE;
    /* we don't have any data to send.  fill payload with 0's */
    for (i = 0; i < 20; i++)
      sendpkt.payload[i] = '0';
    /* computer checksum */
    sendpkt.checksum = ComputeChecksum(sendpkt);
    /*send ack*/
    tolayer3(B, sendpkt);
    /* need to check if new packet or duplicate */
    seqfirst = baseseqnum_b;
    seqlast = (baseseqnum_b + WINDOWSIZE-1) % SEQSPACE;

    /*see if the packet received is inside the window*/
    if (((seqfirst <= seqlast) && (packet.seqnum >= seqfirst && packet.seqnum <= seqlast)) ||
        ((seqfirst > seqlast) && (packet.seqnum >= seqfirst || packet.seqnum <= seqlast)))
    {

      /*get index*/
      if (packet.seqnum >= seqfirst)
        index = packet.seqnum - seqfirst;
      else
        index = WINDOWSIZE - seqfirst + packet.seqnum;
      /*keep receivelast */
      receivelast = receivelast > index ? receivelast:index;

      /*if not duplicate, save to buffer*/

      if (strcmp(buffer_b[index].payload, packet.payload) !=0)
      {
        /*buffer it*/
        packet.acknum = packet.seqnum;
        buffer_b[index] = packet;
        /*if it is the base*/
        if (packet.seqnum == seqfirst){
          for (i = 0; i < WINDOWSIZE; i++)
          {
            if (buffer_b[i].acknum >= 0 && strcmp(buffer_b[i].payload, "")!= 0)
              pckcount++;
            else
              break;
          }
          /* update state variables */
          baseseqnum_b = (baseseqnum_b + pckcount) % SEQSPACE;
          /*update buffer*/
          for (i = 0; i <WINDOWSIZE; i++)
          {
            if ((i + pckcount) <= (receivelast+1))
              buffer_b[i] = buffer_b[i + pckcount];
          }

        }
        /* deliver to receiving application */
        tolayer5(B, packet.payload);
      }
    }
  }
}
/* the following routine will be called once (only) before any other */
/* entity B routines are called. You can use it to do any initialization */
void B_init(void)
{
  /* initialise B's window, buffer and sequence number */
  baseseqnum_b = 0;   /*record the first seq num of the window*/
  receivelast = -1;
}
    /******************************************************************************
     * The following functions need be completed only for bi-directional messages *
     *****************************************************************************/

    /* Note that with simplex transfer from a-to-B, there is no B_output() */
    void B_output(struct msg message)
    {
    }

    /* called when B's timer goes off */
    void B_timerinterrupt(void)
    {
    }

