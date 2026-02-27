'use client';

import { useState, useEffect } from 'react';

interface UserInfo {
  uid?: string | null;
  name?: string | null;
  email?: string | null;
  realm?: string | null;
  authenticated: boolean;
  hasToken: boolean;
  debug?: any;
  error?: string;
}

export function UserInfo() {
  const [userInfo, setUserInfo] = useState<UserInfo>({ authenticated: false, hasToken: false });
  const [loading, setLoading] = useState(true);
  const [showDebug, setShowDebug] = useState(false);

  useEffect(() => {
    // Fetch user info from a special endpoint that extracts Fabric Gateway headers
    fetch('/api/auth/user-info')
      .then(response => response.json())
      .then(data => {
        console.log('User info response:', data);
        setUserInfo(data);
        setLoading(false);
      })
      .catch(error => {
        console.error('Failed to fetch user info:', error);
        setUserInfo({ authenticated: false, hasToken: false, error: error.message });
        setLoading(false);
      });
  }, []);

  if (loading) {
    return (
      <div className="flex items-center space-x-2">
        <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-gray-600"></div>
        <span className="text-sm text-gray-600">Loading...</span>
      </div>
    );
  }

  const getStatusColor = () => {
    if (userInfo.authenticated && userInfo.hasToken) return 'bg-green-500';
    if (userInfo.authenticated || userInfo.hasToken) return 'bg-yellow-500';
    return 'bg-gray-500';
  };

  const getStatusText = () => {
    if (userInfo.authenticated && userInfo.hasToken) return 'Fully Authenticated';
    if (userInfo.authenticated) return 'User ID Found';
    if (userInfo.hasToken) return 'Token Found';
    return 'No Auth Headers';
  };

  return (
    <div className="flex items-center space-x-2">
      <div
        className={`w-8 h-8 ${getStatusColor()} rounded-full flex items-center justify-center text-white text-sm font-semibold cursor-pointer`}
        onClick={() => setShowDebug(!showDebug)}
        title="Click to toggle debug info"
      >
        {userInfo.uid?.charAt(0).toUpperCase() || '?'}
      </div>

      <div className="flex flex-col">
        <span className="text-sm font-medium text-gray-900">
          {userInfo.name || userInfo.uid || 'Unknown User'}
        </span>
        <span className="text-xs text-gray-500">{getStatusText()}</span>
        {userInfo.realm && (
          <span className="text-xs text-gray-400">{userInfo.realm}</span>
        )}
      </div>

      {showDebug && (
        <div className="fixed top-16 right-4 bg-white border shadow-lg rounded-lg p-4 z-50 max-w-md max-h-96 overflow-auto">
          <div className="flex justify-between items-center mb-2">
            <h3 className="font-semibold text-sm">Debug Info</h3>
            <button
              onClick={() => setShowDebug(false)}
              className="text-gray-400 hover:text-gray-600"
            >
              ✕
            </button>
          </div>

          <div className="text-xs space-y-2">
            <div>
              <strong>Auth Status:</strong>
              <div className="ml-2">
                <div>Authenticated: {userInfo.authenticated ? '✅' : '❌'}</div>
                <div>Has Token: {userInfo.hasToken ? '✅' : '❌'}</div>
                <div>User ID: {userInfo.uid || 'None'}</div>
                <div>Realm: {userInfo.realm || 'None'}</div>
              </div>
            </div>

            {userInfo.debug && (
              <div>
                <strong>Fabric Headers:</strong>
                <div className="ml-2">
                  {Object.entries(userInfo.debug.fabricHeaders).map(([key, value]) => (
                    <div key={key}>{key}: {value as string}</div>
                  ))}
                </div>
              </div>
            )}

            {userInfo.debug && (
              <div>
                <strong>Request Info:</strong>
                <div className="ml-2">
                  <div>Headers Received: {userInfo.debug.headersReceived}</div>
                  <div>Host: {userInfo.debug.host}</div>
                  <div>Origin: {userInfo.debug.origin}</div>
                </div>
              </div>
            )}

            {userInfo.error && (
              <div>
                <strong>Error:</strong>
                <div className="ml-2 text-red-600">{userInfo.error}</div>
              </div>
            )}
          </div>
        </div>
      )}
    </div>
  );
}