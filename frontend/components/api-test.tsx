'use client';

import { useState } from 'react';
import { apiClient } from '@/lib/api';
import { setAuthTokenForTesting, getAuthHeader } from '@/lib/auth-utils';

interface ApiTestProps {
  className?: string;
}

export function ApiTest({ className }: ApiTestProps) {
  const [results, setResults] = useState<string>('');
  const [testToken, setTestToken] = useState('');

  const log = (message: string) => {
    setResults(prev => prev + '\n' + new Date().toLocaleTimeString() + ': ' + message);
  };

  const clearLogs = () => {
    setResults('');
  };

  const setToken = () => {
    if (!testToken.trim()) {
      log('❌ Please enter a test token');
      return;
    }

    setAuthTokenForTesting(testToken);
    log(`✅ Token set successfully`);
    log(`Auth header: ${getAuthHeader()}`);
  };

  const testConfig = async () => {
    try {
      log('🔄 Testing GET /api/config...');
      const config = await apiClient.getConfiguration();
      log(`✅ Config API success: ${JSON.stringify(config).substring(0, 100)}...`);
    } catch (error) {
      log(`❌ Config API failed: ${error}`);
    }
  };

  const testChat = async () => {
    try {
      log('🔄 Testing POST /api/chat/ask...');
      const response = await apiClient.sendMessage({
        question: 'Test question',
        maxResults: 3,
      });
      log(`✅ Chat API success: ${JSON.stringify(response).substring(0, 100)}...`);
    } catch (error) {
      log(`❌ Chat API failed: ${error}`);
    }
  };

  const testDocuments = async () => {
    try {
      log('🔄 Testing GET /api/documents...');
      const documents = await apiClient.getDocuments();
      log(`✅ Documents API success: Found ${documents.length} documents`);
    } catch (error) {
      log(`❌ Documents API failed: ${error}`);
    }
  };

  const testAnalytics = async () => {
    try {
      log('🔄 Testing GET /api/analytics/dashboard...');
      const dashboard = await apiClient.getAnalyticsDashboard();
      log(`✅ Analytics API success: ${JSON.stringify(dashboard).substring(0, 100)}...`);
    } catch (error) {
      log(`❌ Analytics API failed: ${error}`);
    }
  };

  return (
    <div className={`bg-white shadow-md rounded-lg p-6 ${className}`}>
      <h3 className="text-lg font-semibold mb-4">API Proxy Test</h3>

      <div className="space-y-4">
        {/* Token Input */}
        <div className="flex gap-2">
          <input
            type="text"
            placeholder="Enter test token (Bearer token without 'Bearer ' prefix)"
            value={testToken}
            onChange={(e) => setTestToken(e.target.value)}
            className="flex-1 px-3 py-2 border rounded-md"
          />
          <button
            onClick={setToken}
            className="px-4 py-2 bg-blue-500 text-white rounded hover:bg-blue-600"
          >
            Set Token
          </button>
        </div>

        {/* Test Buttons */}
        <div className="flex flex-wrap gap-2">
          <button
            onClick={testConfig}
            className="px-4 py-2 bg-green-500 text-white rounded hover:bg-green-600"
          >
            Test Config API
          </button>
          <button
            onClick={testChat}
            className="px-4 py-2 bg-purple-500 text-white rounded hover:bg-purple-600"
          >
            Test Chat API
          </button>
          <button
            onClick={testDocuments}
            className="px-4 py-2 bg-orange-500 text-white rounded hover:bg-orange-600"
          >
            Test Documents API
          </button>
          <button
            onClick={testAnalytics}
            className="px-4 py-2 bg-teal-500 text-white rounded hover:bg-teal-600"
          >
            Test Analytics API
          </button>
          <button
            onClick={clearLogs}
            className="px-4 py-2 bg-gray-500 text-white rounded hover:bg-gray-600"
          >
            Clear Logs
          </button>
        </div>

        {/* Results */}
        {results && (
          <pre className="bg-gray-100 p-4 rounded-md text-sm font-mono whitespace-pre-wrap max-h-96 overflow-y-auto">
            {results}
          </pre>
        )}

        {/* Instructions */}
        <div className="text-sm text-gray-600">
          <p><strong>Instructions:</strong></p>
          <ol className="list-decimal ml-4 space-y-1">
            <li>Set your authentication token (without "Bearer " prefix)</li>
            <li>Click test buttons to verify proxy APIs work</li>
            <li>Check that calls go: Frontend → /api/* → Backend:8080/api/*</li>
            <li>Verify authentication headers are properly forwarded</li>
          </ol>
        </div>
      </div>
    </div>
  );
}