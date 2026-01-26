import React, { useEffect, useState } from 'react'
import {
  Box,
  Paper,
  Typography,
  Avatar,
  TextField,
  Button,
  Grid,
  Alert,
  CircularProgress,
  Divider,
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  IconButton,
} from '@mui/material'
import { PhotoCamera } from '@mui/icons-material'
import { useAuth } from '../context/AuthContext'
import api from '../services/api'

const ProfilePage = () => {
  const { user } = useAuth()
  const [profile, setProfile] = useState(null)
  const [loading, setLoading] = useState(true)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState('')
  const [success, setSuccess] = useState('')
  const [username, setUsername] = useState('')
  const [nickname, setNickname] = useState('')
  const [avatarUrl, setAvatarUrl] = useState('')
  const [changeAvatarOpen, setChangeAvatarOpen] = useState(false)
  const [newAvatarUrl, setNewAvatarUrl] = useState('')
  const [selectedFile, setSelectedFile] = useState(null)
  const [previewUrl, setPreviewUrl] = useState('')
  const [changePasswordOpen, setChangePasswordOpen] = useState(false)
  const [passwordData, setPasswordData] = useState({
    currentPassword: '',
    newPassword: '',
    confirmPassword: '',
  })
  const [passwordError, setPasswordError] = useState('')

  useEffect(() => {
    fetchProfile()
  }, [])

  const fetchProfile = async () => {
    try {
      setLoading(true)
      const response = await api.get('/api/profile')
      setProfile(response.data)
      setUsername(response.data.username || '')
      setNickname(response.data.nickname || '')
      setAvatarUrl(response.data.avatarUrl || '')
      setError('')
    } catch (err) {
      setError('Не удалось загрузить профиль')
      console.error(err)
    } finally {
      setLoading(false)
    }
  }

  const handleSaveProfile = async () => {
    if (!username || username.trim().length < 3) {
      setError('Логин должен содержать минимум 3 символа')
      return
    }
    
    if (!nickname || nickname.trim().length < 3) {
      setError('Никнейм должен содержать минимум 3 символа')
      return
    }
    
    try {
      setSaving(true)
      setError('')
      setSuccess('')
      await api.put('/api/profile', { 
        username: username.trim(),
        nickname: nickname.trim()
      })
      setSuccess('Профиль успешно обновлен')
      await fetchProfile()
    } catch (err) {
      const errorMessage = err.response?.data?.error || 
                          err.response?.data?.message || 
                          'Не удалось обновить профиль'
      setError(errorMessage)
      console.error(err)
    } finally {
      setSaving(false)
    }
  }

  const handleFileSelect = (event) => {
    const file = event.target.files[0]
    if (file) {
      // Валидация типа файла
      const validTypes = ['image/jpeg', 'image/jpg', 'image/png', 'image/gif', 'image/webp']
      if (!validTypes.includes(file.type)) {
        setError('Неподдерживаемый тип файла. Разрешены только изображения: JPEG, PNG, GIF, WEBP')
        return
      }
      
      // Валидация размера файла (5 МБ)
      if (file.size > 5 * 1024 * 1024) {
        setError('Размер файла не должен превышать 5 МБ')
        return
      }
      
      setSelectedFile(file)
      setNewAvatarUrl('') // Сбрасываем URL при выборе файла
      setError('')
      
      // Создаем preview
      const reader = new FileReader()
      reader.onloadend = () => {
        setPreviewUrl(reader.result)
      }
      reader.readAsDataURL(file)
    }
  }

  const handleChangeAvatar = async () => {
    // Если выбран файл, загружаем его
    if (selectedFile) {
      try {
        setSaving(true)
        setError('')
        setSuccess('')
        
        const formData = new FormData()
        formData.append('file', selectedFile)
        
        await api.post('/api/profile/avatar', formData, {
          headers: {
            'Content-Type': 'multipart/form-data',
          },
        })
        
        setSuccess('Аватар успешно обновлен')
        setChangeAvatarOpen(false)
        setSelectedFile(null)
        setPreviewUrl('')
        setNewAvatarUrl('')
        await fetchProfile()
      } catch (err) {
        const errorMessage = err.response?.data?.error || 
                            err.response?.data?.message || 
                            'Не удалось обновить аватар'
        setError(errorMessage)
        console.error(err)
      } finally {
        setSaving(false)
      }
    } 
    // Если введен URL, используем старый метод
    else if (newAvatarUrl.trim()) {
      try {
        setSaving(true)
        setError('')
        setSuccess('')
        await api.put('/api/profile', { avatarUrl: newAvatarUrl.trim() })
        setSuccess('Аватар успешно обновлен')
        setChangeAvatarOpen(false)
        setNewAvatarUrl('')
        await fetchProfile()
      } catch (err) {
        const errorMessage = err.response?.data?.error || 
                            err.response?.data?.message || 
                            'Не удалось обновить аватар'
        setError(errorMessage)
        console.error(err)
      } finally {
        setSaving(false)
      }
    } else {
      setError('Выберите файл или введите URL изображения')
    }
  }

  const handleChangePassword = async () => {
    // Валидация
    if (!passwordData.currentPassword) {
      setPasswordError('Введите текущий пароль')
      return
    }

    if (!passwordData.newPassword) {
      setPasswordError('Введите новый пароль')
      return
    }

    if (passwordData.newPassword.length < 6) {
      setPasswordError('Новый пароль должен содержать минимум 6 символов')
      return
    }

    if (passwordData.newPassword !== passwordData.confirmPassword) {
      setPasswordError('Новые пароли не совпадают')
      return
    }

    try {
      setPasswordError('')
      const response = await api.post('/api/profile/change-password', {
        currentPassword: passwordData.currentPassword,
        newPassword: passwordData.newPassword,
      })
      
      setChangePasswordOpen(false)
      setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' })
      setSuccess(response.data?.message || 'Пароль успешно изменен')
    } catch (err) {
      let errorMessage = 'Не удалось изменить пароль'
      
      if (err.response?.data) {
        // Обработка ошибок валидации от Spring (ErrorDto)
        if (err.response.data.error) {
          errorMessage = err.response.data.error
        } else if (err.response.data.message) {
          errorMessage = err.response.data.message
        } else if (typeof err.response.data === 'string') {
          errorMessage = err.response.data
        } else if (err.response.status === 400) {
          // Ошибка валидации от Spring
          errorMessage = 'Ошибка валидации данных. Проверьте правильность введенных данных.'
        }
      } else if (err.message) {
        errorMessage = err.message
      }
      
      setPasswordError(errorMessage)
      console.error('Ошибка смены пароля:', err)
    }
  }

  if (loading) {
    return (
      <Box display="flex" justifyContent="center" alignItems="center" minHeight="400px">
        <CircularProgress />
      </Box>
    )
  }

  return (
    <Box>
      <Typography variant="h4" gutterBottom>
        Профиль пользователя
      </Typography>

      {error && (
        <Alert severity="error" sx={{ mb: 2 }} onClose={() => setError('')}>
          {error}
        </Alert>
      )}

      {success && (
        <Alert severity="success" sx={{ mb: 2 }} onClose={() => setSuccess('')}>
          {success}
        </Alert>
      )}

      <Grid container spacing={3} sx={{ mt: 2 }}>
        <Grid item xs={12} md={4}>
          <Paper sx={{ p: 3, textAlign: 'center' }}>
            <Box sx={{ position: 'relative', display: 'inline-block', mb: 2 }}>
              <Avatar
                src={
                  profile?.avatarUrl && 
                  profile.avatarUrl !== '/images/default-avatar.png' &&
                  profile.avatarUrl !== null &&
                  !profile.avatarUrl.startsWith('/images/')
                    ? profile.avatarUrl 
                    : '/images/default-avatar.png'
                }
                sx={{
                  width: 150,
                  height: 150,
                  fontSize: 60,
                  bgcolor: 'primary.main',
                }}
                onError={(e) => {
                  // Если загрузка не удалась, используем дефолтную картинку
                  if (!e.target.src.endsWith('/images/default-avatar.png')) {
                    e.target.src = '/images/default-avatar.png'
                  }
                }}
              >
                {profile?.nickname?.charAt(0)?.toUpperCase() || profile?.username?.charAt(0)?.toUpperCase() || 'U'}
              </Avatar>
              <IconButton
                color="primary"
                aria-label="изменить аватар"
                onClick={() => setChangeAvatarOpen(true)}
                sx={{
                  position: 'absolute',
                  bottom: 0,
                  right: 0,
                  bgcolor: 'background.paper',
                  '&:hover': {
                    bgcolor: 'action.hover',
                  },
                }}
              >
                <PhotoCamera />
              </IconButton>
            </Box>
            <Typography variant="h6">{profile?.nickname || profile?.username}</Typography>
          </Paper>
        </Grid>

        <Grid item xs={12} md={8}>
          <Paper sx={{ p: 3 }}>
            <Typography variant="h6" gutterBottom>
              Данные пользователя
            </Typography>
            <Divider sx={{ mb: 3 }} />

            <TextField
              fullWidth
              label="Логин"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              sx={{ mb: 2 }}
              helperText="Логин для входа в систему (минимум 3 символа)"
              inputProps={{ minLength: 3, maxLength: 50 }}
            />

            <TextField
              fullWidth
              label="Никнейм"
              value={nickname}
              onChange={(e) => setNickname(e.target.value)}
              sx={{ mb: 2 }}
              helperText="Отображаемое имя (минимум 3 символа)"
              inputProps={{ minLength: 3, maxLength: 50 }}
            />

            <Box sx={{ display: 'flex', gap: 2, mb: 3 }}>
              <Button
                variant="contained"
                onClick={handleSaveProfile}
                disabled={saving}
              >
                {saving ? <CircularProgress size={24} /> : 'Сохранить изменения'}
              </Button>
            </Box>

            <Divider sx={{ my: 3 }} />

            <Typography variant="h6" gutterBottom>
              Безопасность
            </Typography>

            <Button
              variant="outlined"
              onClick={() => setChangePasswordOpen(true)}
            >
              Изменить пароль
            </Button>
          </Paper>
        </Grid>
      </Grid>

      <Dialog
        open={changeAvatarOpen}
        onClose={() => {
          setChangeAvatarOpen(false)
          setNewAvatarUrl('')
          setSelectedFile(null)
          setPreviewUrl('')
          setError('')
        }}
        maxWidth="sm"
        fullWidth
      >
        <DialogTitle>Изменение аватара</DialogTitle>
        <DialogContent>
          <Box sx={{ mb: 3 }}>
            <Typography variant="subtitle2" gutterBottom>
              Выберите файл с компьютера:
            </Typography>
            <Button
              variant="outlined"
              component="label"
              fullWidth
              startIcon={<PhotoCamera />}
              sx={{ mb: 2 }}
            >
              Выбрать файл
              <input
                type="file"
                hidden
                accept="image/jpeg,image/jpg,image/png,image/gif,image/webp"
                onChange={handleFileSelect}
              />
            </Button>
            {selectedFile && (
              <Typography variant="body2" color="textSecondary" sx={{ mb: 2 }}>
                Выбран файл: {selectedFile.name} ({(selectedFile.size / 1024).toFixed(2)} КБ)
              </Typography>
            )}
          </Box>

          <Divider sx={{ my: 2 }}>
            <Typography variant="body2" color="textSecondary">
              или
            </Typography>
          </Divider>

          <Box>
            <Typography variant="subtitle2" gutterBottom>
              Введите URL изображения:
            </Typography>
            <TextField
              fullWidth
              label="URL изображения"
              value={newAvatarUrl}
              onChange={(e) => {
                setNewAvatarUrl(e.target.value)
                if (e.target.value) {
                  setSelectedFile(null) // Сбрасываем файл при вводе URL
                  setPreviewUrl('')
                }
              }}
              placeholder="https://example.com/avatar.jpg"
              sx={{ mt: 1 }}
              helperText="Введите URL изображения для аватара"
            />
          </Box>

          {(previewUrl || newAvatarUrl) && (
            <Box sx={{ mt: 3, textAlign: 'center' }}>
              <Typography variant="body2" color="textSecondary" sx={{ mb: 1 }}>
                Предпросмотр:
              </Typography>
              <Avatar
                src={previewUrl || newAvatarUrl}
                sx={{
                  width: 100,
                  height: 100,
                  mx: 'auto',
                  fontSize: 40,
                  bgcolor: 'primary.main',
                }}
                onError={(e) => {
                  if (!e.target.src.endsWith('/images/default-avatar.png')) {
                    e.target.src = '/images/default-avatar.png'
                  }
                }}
              >
                {profile?.nickname?.charAt(0)?.toUpperCase() || profile?.username?.charAt(0)?.toUpperCase() || 'U'}
              </Avatar>
            </Box>
          )}
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setChangeAvatarOpen(false)
              setNewAvatarUrl('')
              setSelectedFile(null)
              setPreviewUrl('')
              setError('')
            }}
          >
            Отмена
          </Button>
          <Button onClick={handleChangeAvatar} variant="contained" disabled={saving}>
            {saving ? <CircularProgress size={24} /> : 'Сохранить'}
          </Button>
        </DialogActions>
      </Dialog>

      <Dialog
        open={changePasswordOpen}
        onClose={() => {
          setChangePasswordOpen(false)
          setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' })
          setPasswordError('')
        }}
      >
        <DialogTitle>Изменение пароля</DialogTitle>
        <DialogContent>
          {passwordError && (
            <Alert severity="error" sx={{ mb: 2 }}>
              {passwordError}
            </Alert>
          )}
          <TextField
            fullWidth
            type="password"
            label="Текущий пароль"
            value={passwordData.currentPassword}
            onChange={(e) =>
              setPasswordData({ ...passwordData, currentPassword: e.target.value })
            }
            sx={{ mb: 2, mt: 2 }}
          />
          <TextField
            fullWidth
            type="password"
            label="Новый пароль"
            value={passwordData.newPassword}
            onChange={(e) =>
              setPasswordData({ ...passwordData, newPassword: e.target.value })
            }
            sx={{ mb: 2 }}
            helperText="Минимум 6 символов"
          />
          <TextField
            fullWidth
            type="password"
            label="Подтвердите новый пароль"
            value={passwordData.confirmPassword}
            onChange={(e) =>
              setPasswordData({ ...passwordData, confirmPassword: e.target.value })
            }
          />
        </DialogContent>
        <DialogActions>
          <Button
            onClick={() => {
              setChangePasswordOpen(false)
              setPasswordData({ currentPassword: '', newPassword: '', confirmPassword: '' })
              setPasswordError('')
            }}
          >
            Отмена
          </Button>
          <Button onClick={handleChangePassword} variant="contained">
            Изменить пароль
          </Button>
        </DialogActions>
      </Dialog>
    </Box>
  )
}

export default ProfilePage
