import React, { useState, useEffect } from 'react';
import { 
  Card, 
  List, 
  Button, 
  message,
  Typography,
  Spin,
  Space,
  Divider
} from 'antd';
import { 
  ClockCircleOutlined,
  LinkOutlined
} from '@ant-design/icons';
import { Article, PageResponse } from '../types';
import { articleApi } from '../services/api';
import dayjs from 'dayjs';

const { Title, Paragraph, Text } = Typography;

const MainContent: React.FC = () => {
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(false);

  // 获取文章数据
  const fetchArticles = async () => {
    setLoading(true);
    try {
      const response = await articleApi.getArticles({ page: 0, size: 20, status: 'PUBLISHED' });
      const data: PageResponse<Article> = response.data;
      setArticles(data.content);
    } catch (error) {
      message.error('获取文章列表失败');
      console.error('获取文章失败:', error);
    } finally {
      setLoading(false);
    }
  };

  const initMockData = async () => {
    setLoading(true);
    try {
      await articleApi.initMockData();
      message.success('模拟数据初始化成功');
      await fetchArticles();
    } catch (error) {
      message.error('初始化模拟数据失败');
      console.error('初始化失败:', error);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchArticles();
  }, []);

  const formatTimeAgo = (time: string) => {
    const now = dayjs();
    const publishTime = dayjs(time);
    const diff = now.diff(publishTime, 'hour');
    
    if (diff < 1) return '刚刚';
    if (diff < 24) return `${diff} hours ago`;
    return `${Math.floor(diff / 24)} days ago`;
  };

  return (
    <div style={{ 
      background: '#fff', 
      minHeight: '100vh',
      maxWidth: '800px',
      margin: '0 auto',
      padding: '40px 24px'
    }}>
      {/* 简洁的标题区域 */}
      <div style={{ 
        textAlign: 'center', 
        marginBottom: 48,
        borderBottom: '1px solid #f0f0f0',
        paddingBottom: 32
      }}>
        <Title level={1} style={{ 
          margin: 0, 
          marginBottom: 8,
          fontSize: '36px',
          fontWeight: 'normal',
          color: '#000'
        }}>
          {dayjs().format('YYYY-MM-DD')} 全球热门技术日报
        </Title>
        <Paragraph style={{ 
          fontSize: '16px', 
          color: '#666',
          margin: 0
        }}>
          每日自动汇总全球技术领域的热门文章，提供简短摘要。
        </Paragraph>
      </div>

      {/* 初始化按钮 */}
      {articles.length === 0 && (
        <div style={{ textAlign: 'center', marginBottom: 32 }}>
          <Button 
            type="primary" 
            size="large" 
            onClick={initMockData}
            loading={loading}
          >
            初始化示例数据
          </Button>
        </div>
      )}

      {/* 文章列表 */}
      <Spin spinning={loading}>
        <List
          dataSource={articles}
          renderItem={(article) => (
            <Card 
              className="article-card"
              style={{ 
                marginBottom: 24,
                borderRadius: 8
              }}
              bodyStyle={{ padding: '24px' }}
            >
              <div>
                <Title 
                  level={4} 
                  style={{ 
                    margin: 0,
                    marginBottom: 8,
                    fontSize: '20px',
                    fontWeight: '600',
                    color: '#000',
                    lineHeight: '1.4'
                  }}
                >
                  <a 
                    href={article.url} 
                    target="_blank" 
                    rel="noopener noreferrer"
                    className="article-title"
                  >
                    {article.titleZh || article.title}
                  </a>
                  <LinkOutlined 
                    style={{ 
                      marginLeft: 8, 
                      fontSize: 14,
                      color: '#999'
                    }} 
                  />
                </Title>
                
                <div style={{ 
                  display: 'flex',
                  alignItems: 'center',
                  marginBottom: 12,
                  fontSize: '14px',
                  color: '#999'
                }}>
                  <ClockCircleOutlined style={{ marginRight: 4 }} />
                  {formatTimeAgo(article.publishTime)}
                </div>
                
                <Paragraph style={{ 
                  fontSize: '16px',
                  color: '#666',
                  lineHeight: '1.6',
                  margin: 0
                }}>
                  {article.summaryZh || article.summary}
                </Paragraph>
              </div>
            </Card>
          )}
        />
      </Spin>
    </div>
  );
};

export default MainContent; 