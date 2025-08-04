-- Migration to update old status values to new status values
-- This migration only updates the files table since convert_queue_logs will be populated with new status values

-- Update files table with new status values
UPDATE files 
SET status = 'VALIDATED' 
WHERE status = 'QUEUED_AND_VALIDATED';

UPDATE files 
SET status = 'CONVERTING' 
WHERE status = 'QUEUED_AND_CONVERTED';

UPDATE files 
SET status = 'CONVERTED' 
WHERE status = 'SUCCESS';

UPDATE files 
SET status = 'CONVERSION_FAILED' 
WHERE status = 'FAILED' AND format_to IS NOT NULL;

UPDATE files 
SET status = 'VALIDATION_FAILED' 
WHERE status = 'FAILED' AND format_to IS NULL;

-- Note: UPLOADED status remains the same, so no update needed 