// import React from 'react';

// interface LoadingProps {
//   size?: 'sm' | 'md' | 'lg';
//   text?: string;
//   className?: string;
// }

// const Loading: React.FC<LoadingProps> = ({ 
//   size = 'md', 
//   text = 'Loading...', 
//   className = '' 
// }) => {
//   const sizeClasses = {
//     sm: 'w-4 h-4',
//     md: 'w-8 h-8',
//     lg: 'w-12 h-12'
//   };

//   return (
//     <div className={`flex flex-col items-center justify-center space-y-2 ${className}`}>
//       <div 
//         className={`${sizeClasses[size]} border-4 border-primary-200 border-t-primary-600 rounded-full animate-spin`}
//       />
//       {text && (
//         <p className="text-gray-600 text-sm animate-pulse">{text}</p>
//       )}
//     </div>
//   );
// };

// export default Loading;