import React from 'react';

interface TimelineStep {
  label: string;
  completed: boolean;
}

const steps: TimelineStep[] = [
  { label: 'Tải file lên', completed: true },
  { label: 'Đang xử lý', completed: true },
  { label: 'Chuyển đổi định dạng', completed: false },
  { label: 'Tải file về', completed: false },
];

const FileTimeline: React.FC = () => {
  return (
    <div className="flex flex-col w-full py-6">
      {steps.map((step, idx) => (
        <div key={idx} className="flex items-center w-full">
          {/* Dot + line */}
          <div className="flex flex-col items-center">
            <div
              className={`w-5 h-5 rounded-full border-2 flex-shrink-0 border border-1 ${
                step.completed ? 'boder-blue-500 border-blue-500' : 'boder-base-100 boder-gray-300'
              }`}
            >
              {/*icon here*/}
            </div>
            {/* Line dưới dot, trừ bước cuối */}
            {idx < steps.length - 1 && (
              <div className={`w-0.5 h-10 ${steps[idx + 1].completed ? 'bg-blue-500' : 'bg-gray-300'}`}></div>
            )}
          </div>

          {/* label + virtual line  */}
          <div className="flex flex-col items-center ml-2">
              <span className={`text-sm ${step.completed ? 'text-blue-600 font-semibold' : 'text-gray-500'}`}>{step.label}</span>
            {/* Line dưới dot, trừ bước cuối */}
            {idx < steps.length - 1 && (
              <div className={`h-10`}></div>
            )}
          </div>
        </div>
      ))}
    </div>
  );
};

export default FileTimeline;
