/**
 * [场景:WebSocket消息] — sockjs-client + stompjs 连接模板
 *
 * CDP 使用 SockJS + STOMP 协议实现 WebSocket 通信
 * 依赖: sockjs-client + stompjs
 */

import SockJS from "sockjs-client";
import Stomp from "stompjs";
import { getTokenHeader, getRealUrl } from "@/cdp-common/utils/request";

let stompClient: Stomp.Client | null = null;

/**
 * 建立 WebSocket 连接
 */
export function connectWebSocket(
  endpoint: string,        // WebSocket 端点，如 "/ws"
  topic: string,           // 订阅主题，如 "/topic/notifications"
  onMessage: (msg: any) => void,  // 消息回调
  onError?: (error: any) => void
) {
  const url = getRealUrl(endpoint);
  const socket = new SockJS(url);
  stompClient = Stomp.over(socket);

  // 关闭 STOMP 调试日志
  stompClient.debug = () => {};

  const headers = getTokenHeader();

  stompClient.connect(
    headers,
    // 连接成功
    () => {
      console.log("WebSocket connected");

      // 订阅主题
      stompClient?.subscribe(topic, (message) => {
        try {
          const body = JSON.parse(message.body);
          onMessage(body);
        } catch {
          onMessage(message.body);
        }
      });
    },
    // 连接失败
    (error: any) => {
      console.error("WebSocket error:", error);
      onError?.(error);

      // 5 秒后自动重连
      setTimeout(() => {
        connectWebSocket(endpoint, topic, onMessage, onError);
      }, 5000);
    }
  );
}

/**
 * 发送消息
 */
export function sendMessage(destination: string, body: any) {
  if (stompClient?.connected) {
    stompClient.send(destination, {}, JSON.stringify(body));
  } else {
    console.warn("WebSocket not connected");
  }
}

/**
 * 断开连接
 */
export function disconnectWebSocket() {
  if (stompClient?.connected) {
    stompClient.disconnect(() => {
      console.log("WebSocket disconnected");
    });
  }
  stompClient = null;
}

/**
 * 在 Vue 组件中使用:
 *
 * onMounted(() => {
 *   connectWebSocket("/ws", "/topic/notifications", (msg) => {
 *     ElNotification({ title: "新消息", message: msg.content });
 *   });
 * });
 *
 * onBeforeUnmount(() => {
 *   disconnectWebSocket();
 * });
 */
