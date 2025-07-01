import React, { useState, useCallback } from 'react';
import { ConversionStatus } from '@/types';

const Home: React.FC = () => {
  const [dragActive, setDragActive] = useState(false);
  const [selectedFile, setSelectedFile] = useState<File | null>(null);
  const [targetFormat, setTargetFormat] = useState<string>('');
  const [conversionStatus, setConversionStatus] = useState<ConversionStatus | null>(null);

  const handleDrag = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (e.type === 'dragenter' || e.type === 'dragover') {
      setDragActive(true);
    } else if (e.type === 'dragleave') {
      setDragActive(false);
    }
  }, []);

  const handleDrop = useCallback((e: React.DragEvent) => {
    e.preventDefault();
    e.stopPropagation();
    setDragActive(false);
    
    if (e.dataTransfer.files && e.dataTransfer.files[0]) {
      setSelectedFile(e.dataTransfer.files[0]);
    }
  }, []);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      setSelectedFile(e.target.files[0]);
    }
  };

  const handleConvert = async () => {
    if (!selectedFile || !targetFormat) return;
    
    setConversionStatus(ConversionStatus.UPLOADING);
    
    // TODO: Implement actual conversion logic
    console.log('Converting file:', selectedFile.name, 'to', targetFormat);
    
    // Simulate conversion process
    setTimeout(() => {
      setConversionStatus(ConversionStatus.CONVERTED);
    }, 3000);
  };

  const supportedFormats = [
    { value: 'pdf', label: 'PDF' },
    { value: 'docx', label: 'Word Document' },
    { value: 'jpg', label: 'JPEG Image' },
    { value: 'png', label: 'PNG Image' },
    { value: 'mp4', label: 'MP4 Video' },
    { value: 'mp3', label: 'MP3 Audio' },
  ];

  return (
    <div className="space-y-8">
      {/* Hero Section */}
      <div className="text-center">
        <h2 className="text-4xl font-bold text-gray-900 mb-4">
          Convert Your Files Instantly
        </h2>
        <p className="text-xl text-gray-600 mb-8">
          Fast, secure, and free file conversion. No registration required.
        </p>
      </div>

      {/* File Upload Section */}
      <div className="card max-w-2xl mx-auto">
        <div
          className={`border-2 border-dashed rounded-lg p-8 text-center transition-colors ${
            dragActive
              ? 'border-primary-500 bg-primary-50'
              : 'border-gray-300 hover:border-gray-400'
          }`}
          onDragEnter={handleDrag}
          onDragLeave={handleDrag}
          onDragOver={handleDrag}
          onDrop={handleDrop}
        >
          {selectedFile ? (
            <div className="space-y-4">
              <div className="text-green-600 text-lg font-medium">
                📁 {selectedFile.name}
              </div>
              <div className="text-gray-500 text-sm">
                Size: {(selectedFile.size / 1024 / 1024).toFixed(2)} MB
              </div>
              <button
                onClick={() => setSelectedFile(null)}
                className="text-red-600 hover:text-red-700 text-sm"
              >
                Remove file
              </button>
            </div>
          ) : (
            <div className="space-y-4">
              <div className="text-gray-400 text-6xl">📎</div>
              <div>
                <p className="text-lg font-medium text-gray-700">
                  Drop your file here or click to browse
                </p>
                <p className="text-sm text-gray-500 mt-2">
                  Supports documents, images, videos, and audio files
                </p>
              </div>
              <input
                type="file"
                onChange={handleFileSelect}
                className="hidden"
                id="file-upload"
                accept="*/*"
              />
              <label
                htmlFor="file-upload"
                className="btn-primary cursor-pointer inline-block"
              >
                Choose File
              </label>
            </div>
          )}
        </div>

        {/* Format Selection */}
        {selectedFile && (
          <div className="mt-6 space-y-4">
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-2">
                Convert to:
              </label>
              <select
                value={targetFormat}
                onChange={(e) => setTargetFormat(e.target.value)}
                className="w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent"
              >
                <option value="">Select format...</option>
                {supportedFormats.map((format) => (
                  <option key={format.value} value={format.value}>
                    {format.label}
                  </option>
                ))}
              </select>
            </div>

            <button
              onClick={handleConvert}
              disabled={!targetFormat || conversionStatus === ConversionStatus.UPLOADING}
              className="w-full btn-primary disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {conversionStatus === ConversionStatus.UPLOADING ? 'Converting...' : 'Convert File'}
            </button>
          </div>
        )}

        {/* Conversion Status */}
        {conversionStatus && (
          <div className="mt-6 p-4 rounded-lg bg-blue-50 border border-blue-200">
            <div className="flex items-center space-x-2">
              <div className="w-4 h-4 bg-blue-600 rounded-full animate-pulse"></div>
              <span className="text-blue-800 font-medium">
                {conversionStatus === ConversionStatus.UPLOADING && 'Uploading and converting...'}
                {conversionStatus === ConversionStatus.CONVERTED && 'Conversion completed!'}
              </span>
            </div>
            {conversionStatus === ConversionStatus.CONVERTED && (
              <button className="mt-2 btn-primary">
                Download Converted File
              </button>
            )}
          </div>
        )}
      </div>

      {/* Features Section */}
      <div className="grid md:grid-cols-3 gap-6 mt-12">
        <div className="card text-center">
          <div className="text-primary-600 text-3xl mb-4">⚡</div>
          <h3 className="text-lg font-semibold mb-2">Lightning Fast</h3>
          <p className="text-gray-600">
            Convert files in seconds with our optimized processing engine.
          </p>
        </div>
        
        <div className="card text-center">
          <div className="text-primary-600 text-3xl mb-4">🔒</div>
          <h3 className="text-lg font-semibold mb-2">Secure & Private</h3>
          <p className="text-gray-600">
            Files are automatically deleted after 24 hours. Your privacy is our priority.
          </p>
        </div>
        
        <div className="card text-center">
          <div className="text-primary-600 text-3xl mb-4">🎯</div>
          <h3 className="text-lg font-semibold mb-2">High Quality</h3>
          <p className="text-gray-600">
            Maintain original quality with our advanced conversion algorithms.
          </p>
        </div>
      </div>
    </div>
  );
};

export default Home;