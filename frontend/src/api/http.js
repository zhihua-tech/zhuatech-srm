/* Copyright 2026 Shanghai Rujing Zhihua Information Technology Co., Ltd. · https://www.zhuatech.cn/ */
import axios from "axios";
const http = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || "/api",
  timeout: 15000,
});
http.interceptors.request.use((config) => {
  const token = localStorage.getItem("zhuatech-srm-token");
  if (token) config.headers.Authorization = `Bearer ${token}`;
  config.headers["X-Request-Id"] = crypto.randomUUID();
  return config;
});
http.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && location.pathname !== "/login") {
      localStorage.removeItem("zhuatech-srm-token");
      localStorage.removeItem("zhuatech-srm-user");
      localStorage.removeItem("zhuatech-srm-permissions");
      location.assign("/login");
    }
    return Promise.reject(error);
  },
);
export default http;
