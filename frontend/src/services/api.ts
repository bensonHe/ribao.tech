import axios from 'axios';
import { Article, PageResponse, SearchParams } from '../types';

const API_BASE_URL = 'http://localhost:8080/api';

const api = axios.create({
  baseURL: API_BASE_URL,
  timeout: 10000,
});

// 请求拦截器
api.interceptors.request.use(
  (config) => {
    console.log('发送请求:', config.method?.toUpperCase(), config.url);
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// 响应拦截器
api.interceptors.response.use(
  (response) => {
    console.log('收到响应:', response.status, response.config.url);
    return response;
  },
  (error) => {
    console.error('请求错误:', error.message);
    return Promise.reject(error);
  }
);

export const articleApi = {
  // 获取文章列表
  getArticles: (params: SearchParams = {}) => {
    const queryParams = new URLSearchParams();
    if (params.page !== undefined) queryParams.append('page', params.page.toString());
    if (params.size !== undefined) queryParams.append('size', params.size.toString());
    if (params.status) queryParams.append('status', params.status);
    
    return api.get<PageResponse<Article>>(`/articles?${queryParams.toString()}`);
  },

  // 根据ID获取文章
  getArticleById: (id: number) => {
    return api.get<Article>(`/articles/${id}`);
  },

  // 搜索文章
  searchArticles: (keyword: string, page = 0, size = 10) => {
    return api.get<PageResponse<Article>>(`/articles/search?keyword=${encodeURIComponent(keyword)}&page=${page}&size=${size}`);
  },

  // 获取今日文章
  getTodayArticles: () => {
    return api.get<Article[]>('/articles/today');
  },

  // 获取热门文章
  getPopularArticles: () => {
    return api.get<Article[]>('/articles/popular');
  },

  // 初始化模拟数据
  initMockData: () => {
    return api.post<string>('/articles/init-mock-data');
  },
};

export default api; 