import { createFileRoute, Outlet } from '@tanstack/react-router'

export const Route = createFileRoute('/_app/codebases')({
  component: CodebasesLayout,
})

function CodebasesLayout() {
  return <Outlet />
}
