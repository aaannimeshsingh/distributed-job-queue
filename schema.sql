-- Distributed Job Queue Schema
-- PostgreSQL 14+

-- Drop existing tables if they exist
DROP TABLE IF EXISTS jobs CASCADE;

-- Create jobs table
CREATE TABLE jobs (
    id SERIAL PRIMARY KEY,
    payload JSONB NOT NULL,
    status VARCHAR(20) DEFAULT 'pending' CHECK (status IN ('pending', 'processing', 'completed', 'failed')),
    priority INTEGER DEFAULT 0 CHECK (priority >= 0 AND priority <= 10),
    attempts INTEGER DEFAULT 0,
    max_attempts INTEGER DEFAULT 3,
    created_at TIMESTAMP DEFAULT NOW(),
    scheduled_at TIMESTAMP DEFAULT NOW(),
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    error TEXT
);

-- Create indexes for faster queries
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_scheduled ON jobs(scheduled_at);
CREATE INDEX idx_jobs_priority ON jobs(priority DESC);
CREATE INDEX idx_jobs_created ON jobs(created_at);

-- Composite index for the main query pattern
CREATE INDEX idx_jobs_processing ON jobs(status, priority DESC, created_at ASC) 
WHERE status = 'pending';

-- Comments for documentation
COMMENT ON TABLE jobs IS 'Job queue for distributed task processing';
COMMENT ON COLUMN jobs.id IS 'Unique job identifier';
COMMENT ON COLUMN jobs.payload IS 'Job data in JSON format';
COMMENT ON COLUMN jobs.status IS 'Current job status: pending, processing, completed, failed';
COMMENT ON COLUMN jobs.priority IS 'Job priority (0-10, higher = more important)';
COMMENT ON COLUMN jobs.attempts IS 'Number of processing attempts';
COMMENT ON COLUMN jobs.max_attempts IS 'Maximum retry attempts before permanent failure';
COMMENT ON COLUMN jobs.created_at IS 'Timestamp when job was created';
COMMENT ON COLUMN jobs.scheduled_at IS 'Timestamp when job should be processed';
COMMENT ON COLUMN jobs.started_at IS 'Timestamp when processing started';
COMMENT ON COLUMN jobs.completed_at IS 'Timestamp when job completed or failed permanently';
COMMENT ON COLUMN jobs.error IS 'Error message if job failed';

-- Create a view for monitoring
CREATE OR REPLACE VIEW job_stats AS
SELECT 
    status,
    COUNT(*) as count,
    AVG(EXTRACT(EPOCH FROM (completed_at - started_at))) as avg_duration_seconds,
    MIN(created_at) as oldest_job,
    MAX(created_at) as newest_job
FROM jobs
GROUP BY status;

COMMENT ON VIEW job_stats IS 'Aggregated statistics for job monitoring';

-- Verify installation
SELECT 'Job queue schema installed successfully!' as message;
SELECT table_name FROM information_schema.tables WHERE table_name = 'jobs';