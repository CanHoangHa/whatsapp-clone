import {AfterViewChecked, Component, ElementRef, OnDestroy, OnInit, ViewChild, viewChild} from '@angular/core';
import {ChatListComponent} from '../../components/chat-list/chat-list.component';
import {ChatResponse} from '../../services/models/chat-response';
import {ChatService} from '../../services/services/chat.service';
import { KeycloakService } from '../../utils/keycloak/keycloak.service';
import { MessageService } from '../../services/services';
import { MessageRequest, MessageResponse } from '../../services/models';
import { DatePipe } from '@angular/common';
import {PickerComponent} from '@ctrl/ngx-emoji-mart'
import { FormsModule } from '@angular/forms';
import { EmojiData } from '@ctrl/ngx-emoji-mart/ngx-emoji';
import * as Stomp from 'stompjs';
import SockJS from 'sockjs-client';
import { Notification } from './notification';

@Component({
  selector: 'app-main',
  imports: [
    ChatListComponent,
    DatePipe,
    PickerComponent,
    FormsModule,

  ],
  templateUrl: './main.component.html',
  styleUrl: './main.component.scss'
})
export class MainComponent implements OnInit, OnDestroy, AfterViewChecked{
  chats: Array<ChatResponse> = [];
  selectedChat: ChatResponse = {};
  chatMessages: MessageResponse[] = [];
  showEmojis: boolean = false;
  messageContent: string = '';
  socketClient: Stomp.Client | null = null;
  @ViewChild('scrollableDiv') scrollableDiv!: ElementRef<HTMLDivElement>;
  private stompSubscriptions: Stomp.Subscription[] = [];

  constructor(
    private chatService: ChatService,
    private keycloakService: KeycloakService,
    private messageService: MessageService
  ) {

  }

  ngOnInit(): void {
    this.getAllChats();
    this.initWebSocket();
  }

  ngOnDestroy(): void {
    this.stompSubscriptions.forEach(sub => sub.unsubscribe());
    this.stompSubscriptions = [];
    if (this.socketClient !== null) {
      this.socketClient.disconnect(() => {
        this.socketClient = null;
        console.log('Disconnected WebSocket');
      });
    }
  }
  
  ngAfterViewChecked(): void {
      this.scrollBottom();
  }

  private scrollBottom() {
    if(this.scrollableDiv) {
      const div = this.scrollableDiv.nativeElement;
      div.scrollTop = div.scrollHeight;
    }
  }

  private getAllChats() {
    this.chatService.getChatsByReceiver()
      .subscribe({
        next: (res) =>{
          this.chats = res;
        }
      })
  }

  logout() {
    this.keycloakService.logout();
  }

  userProfile() {
    this.keycloakService.accountManagement();
  }

  chatSelected(chatResponse: ChatResponse) {
    this.selectedChat = chatResponse;
    this.getAllChatMessages(chatResponse.id as string);
    this.setMessagesToSeen();
    this.selectedChat.unreadCount = 0;
  }

  private getAllChatMessages(chatId: string) {
    this.messageService.getChatMessages({
      'chat-id' : chatId
    }).subscribe({
      next: (messages : MessageResponse[] ) => {
        this.chatMessages = messages;
      }
    })
  }

  private setMessagesToSeen() {
    this.messageService.setMessagesToSeen({
      'chat-id' : this.selectedChat.id as string
    }).subscribe({
      next: () => {}
    });
  }

  private extractFileFromTarget(target: EventTarget| null) : File| null {
    const htmlInputTarget = target as HTMLInputElement;
    if(target === null || htmlInputTarget.files === null) {
      return null;
    }
    return htmlInputTarget.files[0];
  }

  isSelfMessage(message: MessageResponse) {
    return message.senderId === this.keycloakService.userId;
  }

  uploadMedia(target: EventTarget|null) {
    const file = this.extractFileFromTarget(target);
    if (file !== null) {
      const reader = new FileReader();
      reader.onload = () => {
        if(reader.result) {
          const mediaLines = reader.result.toString().split(',')[1];

          this.messageService.uploadMediaMessage({
            'chat-id': this.selectedChat.id as string,
            body: {
              file: file
            }
          }).subscribe({
            next: () => {
              const message: MessageResponse = {
                senderId: this.getSenderId(),
                receiverId: this.getReceiverId(),
                content : 'Attachment',
                type: 'IMAGE',
                state: 'SENT',
                media: [mediaLines],
                createdAt: new Date().toString()
              };
              this.chatMessages.push(message);
            }
          });
        }
      }
      reader.readAsDataURL(file);
    }
  }


  onSelectEmojis(emojiSelected: any) {
    const emoji: EmojiData = emojiSelected.emoji;
    this.messageContent += emoji.native;
  }

  keyDown(event: KeyboardEvent) {
    if(event.key === 'Enter'){
      this.sendMessage()
    }
  }
  
  onClick() {
    this.setMessagesToSeen()
  }

  sendMessage() {
    if(this.messageContent) {
      const messageRequest: MessageRequest = {
        chatId: this.selectedChat.id,
        senderId: this.getSenderId(),
        receiverId: this.getReceiverId(),
        content: this.messageContent,
        messageType: 'TEXT'
      }
      this.messageService.saveMessage({
        body: messageRequest
      }).subscribe({
        next: () => {
          const message: MessageResponse = {
            senderId: this.getSenderId(),
            receiverId: this.getReceiverId(),
            content: this.messageContent,
            type: 'TEXT',
            state: 'SENT',
            createdAt: new Date().toString()
          };
          this.selectedChat.lastMessage = this.messageContent;
          this.chatMessages.push(message);
          this.messageContent = '';
          this.showEmojis = false;
        }
      });
    }
  }

  private getSenderId() {
    if(this.selectedChat.senderId === this.keycloakService.userId){
      return this.selectedChat.senderId as string
    }
    return this.selectedChat.recipientId as string
  }

  private getReceiverId() {
    if(this.selectedChat.senderId === this.keycloakService.userId){
      return this.selectedChat.recipientId as string
    }
    return this.selectedChat.senderId as string
  }

  private initWebSocket() {
    if (this.keycloakService.keycloak.tokenParsed?.sub){
      let ws = new SockJS('http://localhost:8080/ws')
      this.socketClient = Stomp.over(ws);
      const subUrl = `/user/${this.keycloakService.keycloak.tokenParsed?.sub}/chat`;
      this.socketClient.connect({'Authorization' : 'Bearer ' + this.keycloakService.keycloak.token},
          () => {
            const sub1 = this.socketClient!.subscribe(subUrl,
                (message: any) => {
                  const notification : Notification = JSON.parse(message.body);
                  this.handleNotification(notification);
                },
                () => console.error('Error while connecting to webSocket')
            )
            this.stompSubscriptions.push(sub1);

            const sub2 = this.socketClient!.subscribe(
              '/topic/user-status',
              (message: any) => {
                const notification : Notification = JSON.parse(message.body);
                this.handleUserStatus(notification);
              }
            )
            this.stompSubscriptions.push(sub1);
          }
      )
    }
  }

  private handleNotification(notification: Notification) {
    if (!notification) return ;
    if (this.selectedChat && this.selectedChat.id === notification.chatId) {
      switch(notification.type) {
        case 'MESSAGE':
        case 'IMAGE' :
          const message: MessageResponse = {
            senderId: notification.senderId,
            receiverId: notification.receiverId,
            content: notification.content,
            type: notification.messageType,
            media: notification.media,
            createdAt: new Date().toString()
          }
          if(notification.type === 'IMAGE') {
            this.selectedChat.lastMessage = 'Attachment';
          } else{
            this.selectedChat.lastMessage = notification.content;
          }
          this.chatMessages.push(message);
          this.selectedChat.unreadCount? this.selectedChat.unreadCount +=1 : 1;
          break;
          case 'SEEN':
            this.chatMessages.forEach(m => m.state = 'SEEN');
            break;
      }
    } else {
      const destChat = this.chats.find(c => c.id == notification.chatId);
      if(destChat && notification.type !== 'SEEN') {
        if(notification.type === 'MESSAGE') {
          destChat.lastMessage = notification.content;
        }
        else if(notification.type === 'IMAGE') {
          destChat.lastMessage = 'Attachment';
        }
        destChat.lastMessageTime = new Date().toString();
        destChat.unreadCount! += 1;
      } else if(notification.type === 'MESSAGE') {
        const newChat : ChatResponse = {
          id: notification.chatId,
          senderId: notification.senderId,
          recipientId: notification.receiverId,
          lastMessage: notification.content,
          name: notification.chatName,
          unreadCount: 1,
          lastMessageTime: new Date().toString()
        };

        this.chats.unshift(newChat);
      }

    }
  }

  private handleUserStatus(notification: Notification) {
    if (!notification || !notification.senderId) return;
  
    const chatToUpdate = this.chats.find(
      c => c.senderId === notification.senderId
        || c.recipientId === notification.senderId
    );
  
    if (chatToUpdate) {
      if (notification.type === 'ONLINE') {
        chatToUpdate.recipientOnline = true;
      }
      
      if (notification.type === 'OFFLINE') {
        chatToUpdate.recipientOnline = false;
      }
    }
  }
  

}


