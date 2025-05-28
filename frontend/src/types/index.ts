export interface Article {
  id: number;
  title: string;
  titleZh?: string;
  summary?: string;
  summaryZh?: string;
  url: string;
  source: string;
  author?: string;
  publishTime: string;
  createdAt: string;
  likes: number;
  views: number;
  tags?: string;
  status: string;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
}

export interface DailyReport {
  id: number;
  reportDate: string;
  title: string;
  summary: string;
  content: string;
  highlights: string;
  trends: string;
  totalArticles: number;
  readCount: number;
  status: string;
  generatedAt: string;
  createdAt: string;
}

export interface SearchParams {
  keyword?: string;
  page?: number;
  size?: number;
  status?: string;
} 